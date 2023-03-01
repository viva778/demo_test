package com.supcon.orchid.material.superwms.services;

public interface PlatformLoginService {
    void login(String username, String companyCode);

    void login(String personCode);
}
