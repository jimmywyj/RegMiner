package org.regminer.migrate.api;


import org.regminer.migrate.api.core.AbstractTestMigrator;

/**
 * @Author: sxz
 * @Date: 2022/06/09/00:12
 * @Description:
 */
public class TestMigrator extends AbstractTestMigrator {
    
    @Override
    public boolean migrateFromTo() {
        return false;
    }
}
