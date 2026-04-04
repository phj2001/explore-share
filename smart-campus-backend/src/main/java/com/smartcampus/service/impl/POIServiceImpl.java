package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIBoundsResponse;
import com.smartcampus.dto.response.POIImportResult;
import com.smartcampus.dto.response.POIImportRowError;
import com.smartcampus.dto.response.POIMapPointResponse;
import com.smartcampus.dto.response.POIOptionResponse;
import com.smartcampus.dto.response.POIQueryResponse;
import com.smartcampus.dto.response.POIResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.POIService;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class POIServiceImpl implements POIService {

    private static final int MAX_ERROR_ROWS = 20;
    private static final int DEFAULT_OPTION_LIMIT = 20;
    private static final int MAX_OPTION_LIMIT = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_MAP_POINT_LIMIT = 1200;
    private static final int MAX_MAP_POINT_LIMIT = 2000;
    private static final int DEFAULT_SEARCH_RESULT_LIMIT = 300;
    private static final int MAX_SEARCH_RESULT_LIMIT = 800;
    private static final Set<String> NAME_HEADERS = Set.of("name", "名称", "poi名称", "地点名称");
    private static final Set<String> CATEGORY_HEADERS = Set.of("category", "分类", "类别");
    private static final Set<String> DESCRIPTION_HEADERS = Set.of("description", "描述", "简介", "说明");
    private static final Set<String> LATITUDE_HEADERS = Set.of("latitude", "lat", "纬度");
    private static final Set<String> LONGITUDE_HEADERS = Set.of("longitude", "lng", "lon", "经度");

    private final POIRepository poiRepository;

    @Override
    @Transactional
    public POI createPOI(POI poi) {
        normalizePoi(poi);
        poi.setCreatedAt(LocalDateTime.now());
        poi.setUpdatedAt(LocalDateTime.now());
        return poiRepository.save(poi);
    }

    @Override
    public Optional<POI> getPOIById(Long id) {
        return poiRepository.findById(id);
    }

    @Override
    public POIQueryResponse getAllPOIs(Integer limit) {
        int safeLimit = normalizeSearchResultLimit(limit);
        PageRequest pageable = PageRequest.of(0, safeLimit);
        long total = poiRepository.count();

        List<POIMapPointResponse> records = poiRepository.findAllMapPointFields(pageable).stream()
                .map(this::toMapPointResponse)
                .toList();

        return new POIQueryResponse(records, total, safeLimit, total > safeLimit);
    }

    @Override
    @Transactional
    public POI updatePOI(POI poi) {
        if (!poiRepository.existsById(poi.getId())) {
            throw new IllegalArgumentException("POI不存在，ID: " + poi.getId());
        }
        normalizePoi(poi);
        poi.setUpdatedAt(LocalDateTime.now());
        return poiRepository.save(poi);
    }

    @Override
    @Transactional
    public void deletePOI(Long id) {
        if (!poiRepository.existsById(id)) {
            throw new IllegalArgumentException("POI不存在，ID: " + id);
        }
        poiRepository.deleteById(id);
    }

    @Override
    public List<POI> searchByName(String name) {
        return poiRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<POI> searchByCategory(String category) {
        return poiRepository.findByCategory(category);
    }

    @Override
    public List<POI> searchByNameAndCategory(String name, String category) {
        return poiRepository.findByNameContainingIgnoreCaseAndCategory(name, category);
    }

    @Override
    public POIQueryResponse getSearchResponse(String name, String category, Integer limit) {
        int safeLimit = normalizeSearchResultLimit(limit);
        PageRequest pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")));

        boolean hasKeyword = StringUtils.hasText(name);
        boolean hasCategory = StringUtils.hasText(category);

        List<POI> pois;
        long total;

        if (hasKeyword && hasCategory) {
            String normalizedName = name.trim();
            String normalizedCategory = category.trim();
            pois = poiRepository.findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(normalizedName, normalizedCategory, pageable);
            total = poiRepository.countByNameContainingIgnoreCaseAndCategory(normalizedName, normalizedCategory);
        } else if (hasKeyword) {
            String normalizedName = name.trim();
            pois = poiRepository.findByNameContainingIgnoreCaseOrderByNameAsc(normalizedName, pageable);
            total = poiRepository.countByNameContainingIgnoreCase(normalizedName);
        } else if (hasCategory) {
            String normalizedCategory = category.trim();
            pois = poiRepository.findByCategoryOrderByNameAsc(normalizedCategory, pageable);
            total = poiRepository.countByCategory(normalizedCategory);
        } else {
            return new POIQueryResponse(List.of(), 0, safeLimit, false);
        }

        return new POIQueryResponse(
                pois.stream().map(this::toMapPointResponse).toList(),
                total,
                safeLimit,
                total > safeLimit
        );
    }

    @Override
    public List<POI> findWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return poiRepository.findWithinBounds(minLat, maxLat, minLng, maxLng);
    }

    @Override
    public List<POIMapPointResponse> findMapPointsWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng, Integer limit) {
        int safeLimit = normalizeMapPointLimit(limit);
        PageRequest pageable = PageRequest.of(0, safeLimit);

        return poiRepository.findWithinBounds(minLat, maxLat, minLng, maxLng, pageable).stream()
                .map(this::toMapPointResponse)
                .toList();
    }

    @Override
    public POIBoundsResponse getBoundsResponse(Double minLat, Double maxLat, Double minLng, Double maxLng, Integer limit) {
        int safeLimit = normalizeMapPointLimit(limit);
        long total = poiRepository.countWithinBounds(minLat, maxLat, minLng, maxLng);
        List<POIMapPointResponse> records = findMapPointsWithinBounds(minLat, maxLat, minLng, maxLng, safeLimit);

        return new POIBoundsResponse(records, total, safeLimit, total > safeLimit);
    }

    @Override
    public List<String> getAllCategories() {
        return poiRepository.findAllCategories();
    }

    @Override
    public PageResponse<POIResponse> getPOIPage(String keyword, String category, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        Page<POI> result = poiRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<POIResponse> records = result.getContent().stream()
                .map(this::toPOIResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    public long countAllPOIs() {
        return poiRepository.count();
    }

    @Override
    public List<POIOptionResponse> searchOptions(String keyword, Integer limit) {
        int safeLimit = normalizeOptionLimit(limit);
        PageRequest pageable = PageRequest.of(0, safeLimit);

        List<POI> pois = StringUtils.hasText(keyword)
                ? poiRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim(), pageable)
                : poiRepository.findAllByOrderByNameAsc(pageable);

        return pois.stream()
                .map(poi -> new POIOptionResponse(poi.getId(), poi.getName(), poi.getCategory()))
                .toList();
    }

    @Override
    @Transactional
    public POIImportResult importFromCsv(MultipartFile file, boolean replaceExisting, boolean skipDuplicates) {
        validateImportFile(file);

        List<List<String>> rows = parseCsv(file);
        if (rows.isEmpty()) {
            throw new BusinessException(400, "CSV 文件为空，无法导入");
        }

        Map<String, Integer> headerIndexMap = resolveHeaderIndexMap(rows.get(0));
        List<POIImportRowError> errors = new ArrayList<>();
        List<POI> importCandidates = new ArrayList<>();
        Set<String> seenImportSignatures = new HashSet<>();
        Set<String> existingSignatures = replaceExisting ? Set.of() : loadExistingSignatures();

        int duplicateCount = 0;
        int invalidCount = 0;
        int emptyRowCount = 0;

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            int actualRowNumber = rowIndex + 1;

            if (isBlankRow(row)) {
                emptyRowCount++;
                continue;
            }

            try {
                POI poi = buildPoiFromRow(row, headerIndexMap);
                String signature = buildSignature(poi);

                if (!seenImportSignatures.add(signature)) {
                    duplicateCount++;
                    continue;
                }

                if (skipDuplicates && existingSignatures.contains(signature)) {
                    duplicateCount++;
                    continue;
                }

                importCandidates.add(poi);
            } catch (IllegalArgumentException ex) {
                invalidCount++;
                if (errors.size() < MAX_ERROR_ROWS) {
                    errors.add(new POIImportRowError(actualRowNumber, safeGetCell(row, headerIndexMap.get("name")), ex.getMessage()));
                }
            }
        }

        int clearedCount = 0;
        if (replaceExisting && !importCandidates.isEmpty()) {
            clearedCount = Math.toIntExact(poiRepository.count());
            poiRepository.deleteAllInBatch();
        }

        if (!importCandidates.isEmpty()) {
            poiRepository.saveAll(importCandidates);
        }

        int skippedCount = duplicateCount + invalidCount + emptyRowCount;
        return new POIImportResult(
                file.getOriginalFilename(),
                Math.max(rows.size() - 1, 0),
                importCandidates.size(),
                skippedCount,
                duplicateCount,
                invalidCount,
                emptyRowCount,
                clearedCount,
                replaceExisting,
                skipDuplicates,
                errors
        );
    }

    private void normalizePoi(POI poi) {
        if (poi == null) {
            return;
        }

        if (StringUtils.hasText(poi.getName())) {
            poi.setName(poi.getName().trim());
        }

        if (StringUtils.hasText(poi.getCategory())) {
            poi.setCategory(poi.getCategory().trim());
        }

        if (poi.getDescription() != null) {
            poi.setDescription(poi.getDescription().trim());
        }
    }

    private int normalizeOptionLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_OPTION_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_OPTION_LIMIT));
    }

    private int normalizeSearchResultLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_RESULT_LIMIT;
        }
        return Math.max(50, Math.min(limit, MAX_SEARCH_RESULT_LIMIT));
    }

    private int normalizeMapPointLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MAP_POINT_LIMIT;
        }
        return Math.max(100, Math.min(limit, MAX_MAP_POINT_LIMIT));
    }

    private POIResponse toPOIResponse(POI poi) {
        return new POIResponse(
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getDescription(),
                poi.getLatitude(),
                poi.getLongitude(),
                poi.getCreatedAt(),
                poi.getUpdatedAt()
        );
    }

    private POIMapPointResponse toMapPointResponse(POI poi) {
        return new POIMapPointResponse(
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getDescription(),
                poi.getLatitude(),
                poi.getLongitude()
        );
    }

    private POIMapPointResponse toMapPointResponse(Object[] row) {
        return new POIMapPointResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (BigDecimal) row[4],
                (BigDecimal) row[5]
        );
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先选择一个 CSV 文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessException(400, "当前仅支持导入 UTF-8 编码的 CSV 文件");
        }
    }

    private List<List<String>> parseCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            return parseCsvContent(stripBom(content.toString()));
        } catch (IOException e) {
            throw new BusinessException(500, "读取 CSV 文件失败");
        }
    }

    private List<List<String>> parseCsvContent(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);

            if (current == '"') {
                if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    currentCell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (!inQuotes && current == ',') {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                continue;
            }

            if (!inQuotes && (current == '\n' || current == '\r')) {
                if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                continue;
            }

            currentCell.append(current);
        }

        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentCell.toString());
            rows.add(currentRow);
        }

        return rows.stream()
                .filter(row -> row.size() > 1 || row.stream().anyMatch(StringUtils::hasText))
                .toList();
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private Map<String, Integer> resolveHeaderIndexMap(List<String> headerRow) {
        Map<String, Integer> resolved = new HashMap<>();

        for (int index = 0; index < headerRow.size(); index++) {
            String header = normalizeHeader(headerRow.get(index));
            if (NAME_HEADERS.contains(header)) {
                resolved.putIfAbsent("name", index);
            } else if (CATEGORY_HEADERS.contains(header)) {
                resolved.putIfAbsent("category", index);
            } else if (DESCRIPTION_HEADERS.contains(header)) {
                resolved.putIfAbsent("description", index);
            } else if (LATITUDE_HEADERS.contains(header)) {
                resolved.putIfAbsent("latitude", index);
            } else if (LONGITUDE_HEADERS.contains(header)) {
                resolved.putIfAbsent("longitude", index);
            }
        }

        List<String> missingColumns = new ArrayList<>();
        if (!resolved.containsKey("name")) {
            missingColumns.add("name/名称");
        }
        if (!resolved.containsKey("latitude")) {
            missingColumns.add("latitude/纬度");
        }
        if (!resolved.containsKey("longitude")) {
            missingColumns.add("longitude/经度");
        }

        if (!missingColumns.isEmpty()) {
            throw new BusinessException(400, "CSV 缺少必填列：" + String.join("、", missingColumns));
        }

        return resolved;
    }

    private String normalizeHeader(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private boolean isBlankRow(List<String> row) {
        return row == null || row.stream().noneMatch(StringUtils::hasText);
    }

    private POI buildPoiFromRow(List<String> row, Map<String, Integer> headerIndexMap) {
        String name = requireText(safeGetCell(row, headerIndexMap.get("name")), "名称不能为空");
        String category = safeGetCell(row, headerIndexMap.get("category"));
        String description = safeGetCell(row, headerIndexMap.get("description"));
        BigDecimal latitude = parseCoordinate(safeGetCell(row, headerIndexMap.get("latitude")), "纬度格式不正确");
        BigDecimal longitude = parseCoordinate(safeGetCell(row, headerIndexMap.get("longitude")), "经度格式不正确");

        POI poi = new POI();
        poi.setName(limitLength(name, 100, "名称长度不能超过 100 个字符"));
        poi.setCategory(limitLength(StringUtils.hasText(category) ? category.trim() : "未分类", 50, "分类长度不能超过 50 个字符"));
        poi.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        poi.setLatitude(latitude.setScale(7, RoundingMode.HALF_UP));
        poi.setLongitude(longitude.setScale(7, RoundingMode.HALF_UP));
        poi.setCreatedAt(LocalDateTime.now());
        poi.setUpdatedAt(LocalDateTime.now());
        normalizePoi(poi);
        return poi;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String limitLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private BigDecimal parseCoordinate(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String safeGetCell(List<String> row, Integer index) {
        if (index == null || row == null || index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private Set<String> loadExistingSignatures() {
        return poiRepository.findAllSignatureFields().stream()
                .map(this::buildSignature)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private String buildSignature(POI poi) {
        String latitude = poi.getLatitude() == null ? "" : poi.getLatitude().stripTrailingZeros().toPlainString();
        String longitude = poi.getLongitude() == null ? "" : poi.getLongitude().stripTrailingZeros().toPlainString();
        return String.join("|",
                normalizeSignaturePart(poi.getName()),
                normalizeSignaturePart(poi.getCategory()),
                latitude,
                longitude
        );
    }

    private String buildSignature(Object[] row) {
        String latitude = row[2] == null ? "" : ((BigDecimal) row[2]).stripTrailingZeros().toPlainString();
        String longitude = row[3] == null ? "" : ((BigDecimal) row[3]).stripTrailingZeros().toPlainString();
        return String.join("|",
                normalizeSignaturePart((String) row[0]),
                normalizeSignaturePart((String) row[1]),
                latitude,
                longitude
        );
    }

    private String normalizeSignaturePart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
