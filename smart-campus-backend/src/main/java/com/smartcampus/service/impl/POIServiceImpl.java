package com.smartcampus.service.impl;

import com.smartcampus.entity.POI;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.POIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class POIServiceImpl implements POIService {

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
    public List<POI> getAllPOIs() {
        return poiRepository.findAll();
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
    public List<POI> findWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return poiRepository.findWithinBounds(minLat, maxLat, minLng, maxLng);
    }

    @Override
    public List<String> getAllCategories() {
        return poiRepository.findAllCategories();
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
}




