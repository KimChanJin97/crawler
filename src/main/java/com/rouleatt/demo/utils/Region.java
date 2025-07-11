package com.rouleatt.demo.utils;

public enum Region {

    SEOUL("서울특별시", "서울", 126.734086, 37.413294, 127.269311, 37.715133),
    BUSAN("부산광역시", "부산",128.755555, 35.051755, 129.292787, 35.392611),
    DAEGU("대구광역시", "대구", 128.335488, 35.678539, 128.801499, 36.079902),
    INCHEON("인천광역시", "인천", 126.373529, 37.335887, 126.950004, 37.748528),
    GWANGJU("광주광역시", "광주", 126.653462, 35.071907, 126.986607, 35.243399),
    DAEJEON("대전광역시", "대전", 127.267888, 36.193335, 127.540208, 36.481778),
    ULSAN("울산광역시", "울산", 129.047923, 35.278131, 129.464146, 35.691022),
    GYEONGGI("경기도", "경기",126.182794, 36.990600, 127.915085, 38.300788),
    CHUNGBUK("충청북도", "충북",127.267304, 36.097828, 128.175039, 37.276553),
    CHUNGNAM("충청남도", "충남", 126.207274, 35.923552, 127.447642, 37.015091),
    JEONBUK("전라북도", "전북", 126.310972, 35.290175, 127.473454, 36.231551),
    JEONNAM("전라남도", "전남", 126.070594, 34.079351, 127.827079, 35.428879),
    GYEONGBUK("경상북도", "경북", 127.480193, 35.600291, 129.468166, 37.055333),
    GYEONGNAM("경상남도", "경남", 127.487505, 34.488986, 129.112528, 35.903977),
    SEJONG("세종특별자치시","세종", 127.171931, 36.429861, 127.409350, 36.646516),
    JEJU("제주특별자치도", "제주", 126.075024, 33.111115, 126.978388, 33.568837),
    GANGWON("강원특별자치도", "강원", 127.059113, 37.038710, 129.426301, 38.613309);

    private final String fullName;
    private final String shortName;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    Region(String fullName, String shortName, double minX, double minY, double maxX, double maxY) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }
}