package com.supcon.orchid.fooramework.services;

public interface PlatformLoginService {
    void login(String username, String companyCode);

    void login(String personCode);
}
