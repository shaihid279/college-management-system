package com.college.cms.service;

import com.college.cms.model.Placement;
import com.college.cms.repository.PlacementRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placement data: the placement pages of the college site (enggnagar) contain links to a Google Sheet and PDFs.
 * If the sheet is public, it is downloaded as CSV and the companies are loaded.
 * If the site or the sheet is unavailable, the page shows "coming soon".
 */
@Service
@Slf4j
public class PlacementService {
    private static final String UA = "Mozilla/5.0 (compatible; CampusCoreBot/1.0)";
    private static final List<String> PAGES = List.of("/list-of-companies-visited/", "/branch-wise-highest-packages/",
            "/placement-activities/", "/about-training-placement-cell/");

    private final PlacementRepository repo;
    private final TransactionTemplate tx;
    private final String sourceUrl;
    private final boolean syncEnabled;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();

    private volatile String lastStatus = "Not synced yet";
    private volatile boolean sourceOnline = false;
    private volatile String lastAttempt = "-";
    private volatile List<String[]> links = List.of();

    public PlacementService(PlacementRepository repo, PlatformTransactionManager tm,
                            @Value("${app.placement.source-url:}") String sourceUrl,
                            @Value("${app.placement.sync-enabled:true}") boolean syncEnabled) {
        this.repo = repo;
        this.tx = new TransactionTemplate(tm);
        this.sourceUrl = sourceUrl;
        this.syncEnabled = syncEnabled;
    }

    public String getLastStatus() { return lastStatus; }
    public boolean isSourceOnline() { return sourceOnline; }
    public String getLastAttempt() { return lastAttempt; }
    public String getSourceUrl() { return sourceUrl; }
    public List<String[]> getLinks() { return links; }

    @Scheduled(initialDelay = 20000, fixedDelay = 6 * 60 * 60 * 1000L)
    public void scheduledSync() {
        if (syncEnabled) syncFromWebsite();
    }

    public String syncFromWebsite() {
        lastAttempt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        if (sourceUrl == null || sourceUrl.isBlank()) {
            sourceOnline = false;
            return lastStatus = "The source URL is not set.";
        }
        String base = sourceUrl.trim().replaceAll("/+$", "");
        List<String[]> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String sheetUrl = null;
        int reached = 0;
        try {
            for (String path : PAGES) {
                Document doc;
                try {
                    doc = Jsoup.connect(base + path).userAgent(UA).timeout(15000).followRedirects(true).get();
                } catch (Exception e) {
                    continue;
                }
                reached++;
                String t = doc.title() == null ? "" : doc.title().toLowerCase();
                String b = doc.body() == null ? "" : doc.body().text().toLowerCase();
                if (t.contains("suspended") || b.contains("account has been suspended")) {
                    sourceOnline = false;
                    return lastStatus = "The source website is currently suspended - data coming soon.";
                }
                for (Element a : doc.select("a[href]")) {
                    String href = a.absUrl("href");
                    String text = a.text().trim();
                    if (href.isBlank()) continue;
                    boolean sheet = href.contains("docs.google.com/spreadsheets");
                    String low = (href + " " + text).toLowerCase();
                    boolean pdf = href.toLowerCase().endsWith(".pdf")
                            && (low.contains("placement") || low.contains("package") || low.contains("recruiter"));
                    if ((sheet || pdf) && seen.add(href)) {
                        found.add(new String[]{sheet ? "List of Companies visited (Google Sheet)" : pdfTitle(text, href), href});
                        if (sheet && sheetUrl == null) sheetUrl = href;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Placement sync failed: {}", e.getMessage());
        }
        if (reached == 0) {
            sourceOnline = false;
            return lastStatus = "The college website is currently unavailable - data coming soon.";
        }
        sourceOnline = true;
        links = List.copyOf(found);
        if (sheetUrl == null) {
            return lastStatus = "The college site is running, but the companies sheet link was not found.";
        }
        try {
            String csvUrl = csvUrl(sheetUrl);
            if (csvUrl == null) return lastStatus = "Could not understand the sheet link.";
            HttpRequest req = HttpRequest.newBuilder(URI.create(csvUrl)).timeout(Duration.ofSeconds(30))
                    .header("User-Agent", UA).GET().build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            String body = new String(resp.body(), StandardCharsets.UTF_8);
            if (resp.statusCode() != 200 || body.trim().toLowerCase().startsWith("<")) {
                return lastStatus = "The Google Sheet is not public (Share > Anyone with the link). For now, download the sheet as CSV and upload it from Admin tools.";
            }
            List<Placement> rows = toPlacements(parseCsv(body), "WEB");
            if (rows.isEmpty()) {
                return lastStatus = "The sheet was found but it has no company column. Please upload a CSV from Admin tools.";
            }
            tx.executeWithoutResult(s -> {
                repo.deleteAll(repo.findBySource("WEB"));
                repo.saveAll(rows);
            });
            return lastStatus = rows.size() + " company records loaded from the college sheet.";
        } catch (Exception e) {
            log.warn("Sheet download failed: {}", e.getMessage());
            return lastStatus = "The sheet could not be loaded right now. It will be retried shortly.";
        }
    }

    /** CSV / Excel upload. The first row must be a header (names like company, role, package, students placed, year). */
    @Transactional
    public int importFile(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<List<String>> rows = new ArrayList<>();
        if (name.endsWith(".csv")) {
            rows = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } else if (name.endsWith(".xlsx")) {
            DataFormatter fmt = new DataFormatter();
            try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
                Sheet sh = wb.getSheetAt(0);
                for (int i = 0; i <= sh.getLastRowNum(); i++) {
                    Row r = sh.getRow(i);
                    if (r == null) continue;
                    List<String> cells = new ArrayList<>();
                    for (int c = 0; c < Math.max(0, r.getLastCellNum()); c++) {
                        Cell cell = r.getCell(c);
                        cells.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
                    }
                    rows.add(cells);
                }
            }
        } else {
            throw new IllegalArgumentException("Please upload a .csv or .xlsx file only.");
        }
        List<Placement> list = toPlacements(rows, "FILE");
        if (list.isEmpty()) throw new IllegalArgumentException("No company column found in the file (write 'Company' in the header).");
        repo.saveAll(list);
        return list.size();
    }

    // ---------------- helpers ----------------

    private List<Placement> toPlacements(List<List<String>> rows, String source) {
        if (rows.isEmpty()) return List.of();
        int hi = 0;
        for (int i = 0; i < Math.min(rows.size(), 15); i++) {
            String joined = String.join(" ", rows.get(i)).toLowerCase();
            if (joined.contains("company") || joined.contains("organi")) { hi = i; break; }
        }
        List<String> h = new ArrayList<>();
        for (String s : rows.get(hi)) h.add(s.toLowerCase().trim());
        int cCompany = idx(h, "company", "organi", "recruiter");
        if (cCompany < 0) cCompany = firstNonSerial(h);
        int cRole = idx(h, "role", "designation", "post", "job");
        int cPkg = idx(h, "package", "lpa", "ctc", "salary");
        int cCount = idx(h, "no. of", "number of", "selected", "placed", "offers");
        int cYear = idx(h, "academic", "year", "batch", "date");

        Map<String, Placement> agg = new LinkedHashMap<>();
        for (int i = hi + 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String company = cell(r, cCompany);
            if (company.isBlank() || company.equalsIgnoreCase("company")) continue;
            String year = trim(cell(r, cYear), 20);
            String key = company.toLowerCase() + "|" + year;
            Placement p = agg.get(key);
            if (p == null) {
                p = new Placement();
                p.setCompany(trim(company, 150));
                p.setAcademicYear(year);
                p.setJobRole("");
                p.setSource(source);
                agg.put(key, p);
            }
            if (cPkg >= 0) p.setPackageLpa(Math.max(p.getPackageLpa(), num(cell(r, cPkg))));
            if (cCount >= 0) p.setStudentsPlaced(p.getStudentsPlaced() + (int) num(cell(r, cCount)));
            if (p.getJobRole().isBlank()) p.setJobRole(trim(cell(r, cRole), 120));
        }
        return new ArrayList<>(agg.values());
    }

    private int idx(List<String> headers, String... keys) {
        for (int i = 0; i < headers.size(); i++) {
            for (String k : keys) if (headers.get(i).contains(k)) return i;
        }
        return -1;
    }

    private int firstNonSerial(List<String> h) {
        for (int i = 0; i < h.size(); i++) {
            String s = h.get(i);
            if (s.isBlank() || s.startsWith("sr") || s.startsWith("s.") || s.startsWith("sl") || s.equals("no") || s.equals("#")) continue;
            return i;
        }
        return 0;
    }

    private String cell(List<String> r, int i) { return i >= 0 && i < r.size() ? r.get(i).trim() : ""; }

    private String trim(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private double num(String s) {
        if (s == null) return 0;
        Matcher m = Pattern.compile("\\d+(\\.\\d+)?").matcher(s);
        if (!m.find()) return 0;
        try { return Double.parseDouble(m.group()); } catch (NumberFormatException e) { return 0; }
    }

    private String csvUrl(String sheetUrl) {
        Matcher m = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)").matcher(sheetUrl);
        if (!m.find()) return null;
        String gid = "0";
        Matcher g = Pattern.compile("gid=(\\d+)").matcher(sheetUrl);
        if (g.find()) gid = g.group(1);
        return "https://docs.google.com/spreadsheets/d/" + m.group(1) + "/export?format=csv&gid=" + gid;
    }

    private String pdfTitle(String text, String href) {
        if (!text.isBlank() && text.length() <= 90 && !text.toLowerCase().startsWith("click")) return text;
        String file = href.substring(href.lastIndexOf('/') + 1).replaceAll("(?i)\\.pdf$", "");
        try { file = URLDecoder.decode(file, StandardCharsets.UTF_8); } catch (Exception ignored) { }
        return file.replace('-', ' ').replace('_', ' ');
    }

    private List<List<String>> parseCsv(String text) {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cur.toString().trim());
                cur.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cur.toString().trim());
                cur.setLength(0);
                if (hasData(row)) rows.add(row);
                row = new ArrayList<>();
            } else cur.append(c);
        }
        row.add(cur.toString().trim());
        if (hasData(row)) rows.add(row);
        return rows;
    }

    private boolean hasData(List<String> row) {
        for (String s : row) if (!s.isEmpty()) return true;
        return false;
    }
}