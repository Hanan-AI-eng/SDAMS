package service;

import java.util.List;

/**
 * Shared by every report class (owner: S1). Any Reportable can be exported to
 * a text file by ReportExporter without the exporter knowing which report it is.
 */
public interface Reportable {

    /** Short title, e.g. "Revenue Report - November 2026". */
    String getTitle();

    /** The report body as plain-text lines, ready to print or save. */
    List<String> toReportLines();
}
