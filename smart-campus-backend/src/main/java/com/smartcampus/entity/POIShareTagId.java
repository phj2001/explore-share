package com.smartcampus.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIShareTagId implements Serializable {
    private Long share;
    private Long tag;
}
