package de.akquinet.jbosscc.guttenbase.utils;

import de.akquinet.jbosscc.guttenbase.hints.CaseConversionMode;
import de.akquinet.jbosscc.guttenbase.meta.ColumnMetaData;
import de.akquinet.jbosscc.guttenbase.meta.DatabaseMetaData;
import de.akquinet.jbosscc.guttenbase.meta.ForeignKeyMetaData;
import de.akquinet.jbosscc.guttenbase.meta.IndexMetaData;
import de.akquinet.jbosscc.guttenbase.meta.builder.*;
import de.akquinet.jbosscc.guttenbase.tools.schema.DatabaseSchemaScriptCreator;
import de.akquinet.jbosscc.guttenbase.tools.schema.DefaultSchemaColumnTypeMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DatabaseSchemaScriptCreatorTest {
  private final DatabaseMetaData _databaseMetaData = createDatabaseMetaData();
  private final DatabaseSchemaScriptCreator _objectUnderTest = new DatabaseSchemaScriptCreator(_databaseMetaData,
          _databaseMetaData.getSchema(), CaseConversionMode.UPPER, DatabaseSchemaScriptCreator.MAX_ID_LENGTH);

  private DatabaseMetaData createDatabaseMetaData() {
    final DatabaseMetaDataBuilder databaseMetaDataBuilder = new DatabaseMetaDataBuilder();
    final TableMetaDataBuilder tableBuilder1 = createTable(1, databaseMetaDataBuilder);
    final TableMetaDataBuilder tableBuilder2 = createTable(2, databaseMetaDataBuilder);

    final ForeignKeyMetaDataBuilder foreignKeyMetaDataBuilder1 = new ForeignKeyMetaDataBuilder(tableBuilder1)
            .setForeignKeyName("FK_Name").setReferencingColumn(tableBuilder1.getColumn("Name"))
            .setReferencedColumn(tableBuilder2.getColumn("Name"));
    final ForeignKeyMetaDataBuilder foreignKeyMetaDataBuilder2 = new ForeignKeyMetaDataBuilder(tableBuilder2)
            .setForeignKeyName("FK_Name").setReferencingColumn(tableBuilder1.getColumn("Name"))
            .setReferencedColumn(tableBuilder2.getColumn("Name"));

    tableBuilder1.addImportedForeignKey(foreignKeyMetaDataBuilder1);
    tableBuilder2.addExportedForeignKey(foreignKeyMetaDataBuilder2);

    databaseMetaDataBuilder.addTable(tableBuilder1);
    databaseMetaDataBuilder.addTable(tableBuilder2);
    databaseMetaDataBuilder.setSchema("schemaName");

    return databaseMetaDataBuilder.build();
  }

  private TableMetaDataBuilder createTable(final int index, final DatabaseMetaDataBuilder databaseMetaDataBuilder) {
    final TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(databaseMetaDataBuilder)
            .setTableName("My_Table" + index);
    final ColumnMetaDataBuilder primaryKeyBuilder = new ColumnMetaDataBuilder(tableMetaDataBuilder).setColumnName("Id")
            .setColumnTypeName("BIGINT").setNullable(false).setPrimaryKey(true);
    final ColumnMetaDataBuilder nameBuilder = new ColumnMetaDataBuilder(tableMetaDataBuilder).setColumnName("Name")
            .setColumnTypeName("VARCHAR(100)").setNullable(false);

    tableMetaDataBuilder
            .addColumn(primaryKeyBuilder)
            .addColumn(nameBuilder)
            .addIndex(
                    new IndexMetaDataBuilder(tableMetaDataBuilder).setAscending(true).setIndexName("Name_IDX" + index).setUnique(true)
                            .addColumn(nameBuilder));
    return tableMetaDataBuilder;
  }

  @Test
  public void testDDL() throws Exception {
    final List<String> tableStatements = _objectUnderTest.createTableStatements();
    assertEquals(2, tableStatements.size());

    final String createStatement = tableStatements.get(0);

    assertTrue(createStatement, createStatement.startsWith("CREATE TABLE schemaName.MY_TABLE"));
    assertTrue(createStatement, createStatement.contains("ID BIGINT NOT NULL"));
    assertTrue(createStatement, createStatement.contains("NAME VARCHAR(100) NOT NULL"));

    final List<String> indexStatements = _objectUnderTest.createIndexStatements();
    assertEquals(2, indexStatements.size());
    final String indexStatement = indexStatements.get(0);
    assertTrue(indexStatement, indexStatement.startsWith("CREATE UNIQUE INDEX IDX_NAME_IDX1_MY_TABLE1_1 ON schemaName.MY_TABLE1"));
    assertTrue(indexStatement, indexStatement.contains("NAME"));

    final List<String> foreignKeyStatements = _objectUnderTest.createForeignKeyStatements();
    assertEquals(1, foreignKeyStatements.size());
    final String foreignKeyStatement = foreignKeyStatements.get(0);

    assertTrue(foreignKeyStatement,
            foreignKeyStatement.startsWith("ALTER TABLE schemaName.MY_TABLE1 ADD CONSTRAINT FK_MY_TABLE1_NAME_NAME_1 FOREIGN KEY "));
  }

  @Test
  public void testSchemaColumnTypeMapper() throws Exception {
    _objectUnderTest.setColumnTypeMapper(new DefaultSchemaColumnTypeMapper() {
      @Override
      public String getColumnType(final ColumnMetaData columnMetaData) {
        if ("BIGINT".equalsIgnoreCase(columnMetaData.getColumnTypeName())) {
          return "INTEGER";
        } else {
          return super.getColumnType(columnMetaData);
        }
      }
    });
    final List<String> tableStatements = _objectUnderTest.createTableStatements();
    assertEquals(2, tableStatements.size());

    final String createStatement = tableStatements.get(0);

    assertTrue(createStatement, createStatement.startsWith("CREATE TABLE schemaName.MY_TABLE"));
    assertTrue(createStatement, createStatement.contains("ID INTEGER NOT NULL"));
    assertTrue(createStatement, createStatement.contains("NAME VARCHAR(100) NOT NULL"));
  }

  @Test
  public void testCreateConstraintName() throws Exception {
    assertEquals("FK_NAME_1", _objectUnderTest.createConstraintName("FK_", "NAME_", 1));
    final String constraintName = _objectUnderTest.createConstraintName("FK_", "AUFTRAG_STELLUNGNAHME_HALTUNGSTELLUNGNAHME_ZU_HALTUNG_ID_PARENT_ID__ID_", 1);

    assertFalse("FK_AUFTRAG_STELLUNGNAHME_HALTUNGSTELLUNGNAHME_ZU_HALTUNG_ID_PARENT_ID__ID_1".equals(constraintName));
    assertEquals(DatabaseSchemaScriptCreator.MAX_ID_LENGTH, constraintName.length());
  }

  @Test
  public void testForeignKey() throws Exception {
    final ForeignKeyMetaData foreignKeyMetaData = _databaseMetaData.getTableMetaData().get(0).getImportedForeignKeys().get(0);
    final String sql = _objectUnderTest.createForeignKey(foreignKeyMetaData);

    assertEquals("ALTER TABLE schemaName.MY_TABLE1 ADD CONSTRAINT FK_Name FOREIGN KEY (NAME) REFERENCES schemaName.MY_TABLE2(NAME);", sql);
  }

  @Test
  public void createColumn()
  {
    final String sql = _objectUnderTest.createTableColumn(_databaseMetaData.getTableMetaData().get(0).getColumnMetaData().get(1));

    assertEquals("ALTER TABLE schemaName.My_Table1 ADD COLUMN Name VARCHAR(100)",sql);
  }

  @Test
  public void testIndex() throws Exception {
    final ColumnMetaData columnMetaData = _databaseMetaData.getTableMetaData().get(0).getColumnMetaData("name");
    final IndexMetaData index = _databaseMetaData.getTableMetaData().get(0).getIndexesForColumn(columnMetaData).get(0);
    final String sql = _objectUnderTest.createIndex(index);

    assertEquals("CREATE UNIQUE INDEX Name_IDX1 ON schemaName.MY_TABLE1(NAME);", sql);
  }
}
