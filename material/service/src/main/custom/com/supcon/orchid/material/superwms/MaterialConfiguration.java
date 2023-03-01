package com.supcon.orchid.material.superwms;

import com.supcon.orchid.fooramework.lifecycle.FoorameworkConfiguration;
import org.springframework.stereotype.Component;

/**
 * 配置这个包下的Fooramework
 */
@Component
public class MaterialConfiguration implements FoorameworkConfiguration {

    @Override
    public String[] scan_path() {
        return new String[]{"com.supcon.orchid.material.superwms"};
    }
}
