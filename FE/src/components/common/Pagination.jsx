import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const Pagination = ({ currentPage = 0, totalPages = 1, onPageChange }) => {
  if (totalPages <= 1) return null;

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginTop: '1.5rem' }}>
      <button
        className="btn btn-secondary btn-sm"
        disabled={currentPage === 0}
        onClick={() => onPageChange(currentPage - 1)}
      >
        <ChevronLeft size={16} /> Trang trước
      </button>

      <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-muted)', margin: '0 0.5rem' }}>
        Trang {currentPage + 1} / {totalPages}
      </span>

      <button
        className="btn btn-secondary btn-sm"
        disabled={currentPage >= totalPages - 1}
        onClick={() => onPageChange(currentPage + 1)}
      >
        Trang sau <ChevronRight size={16} />
      </button>
    </div>
  );
};

export default Pagination;
