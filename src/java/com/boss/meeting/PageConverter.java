package com.boss.meeting;

import java.io.File;

/**
 * Created by Administrator on 2014/12/13.
 */
public interface PageConverter {
    public boolean convert(File presentation, File output, int page);
}
