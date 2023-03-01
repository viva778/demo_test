package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.fooramework.support.Pair;

import javax.transaction.Transactional;
import java.util.Date;

public interface ModelReportService {

    void doStockReport(Pair<Date, Date> dayRange, Date today);
}
