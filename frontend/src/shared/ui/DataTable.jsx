// Usage: <DataTable columns={[{ key: "code", header: "Code" }]} page={springPage} onPageChange={setPageNo} loading={loading} />
import React from "react";
import EmptyState from "./EmptyState";

/*
 * Works with the Spring Page JSON (C3):
 * { content: [...], number, size, totalElements, totalPages }
 * A plain array also works (no pagination shown).
 * A column can have render(row) for custom cells, e.g. render: row => <StatusBadge status={row.status} />
 */
export default function DataTable({ columns, page, rows, onPageChange, loading = false, emptyText = "No data yet" }) {
  const data = rows || page?.content || [];
  const pageNumber = page?.number ?? 0;
  const totalPages = page?.totalPages ?? 1;

  if (loading) {
    return <p className="p-4 text-sm text-slate-500">Loading...</p>;
  }

  if (data.length === 0) {
    return <EmptyState title={emptyText} />;
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" className="px-4 py-2 text-left font-semibold text-slate-700">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data.map((row, index) => (
            <tr key={row.id ?? index}>
              {columns.map((column) => (
                <td key={column.key} className="px-4 py-2 text-slate-800">
                  {column.render ? column.render(row) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>

      {page && totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-2 text-sm">
          <span>
            Page {pageNumber + 1} of {totalPages} ({page.totalElements} items)
          </span>
          <div className="space-x-2">
            <button
              className="rounded border px-2 py-1 disabled:opacity-50"
              disabled={pageNumber === 0}
              onClick={() => onPageChange(pageNumber - 1)}
            >
              Previous
            </button>
            <button
              className="rounded border px-2 py-1 disabled:opacity-50"
              disabled={pageNumber + 1 >= totalPages}
              onClick={() => onPageChange(pageNumber + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
