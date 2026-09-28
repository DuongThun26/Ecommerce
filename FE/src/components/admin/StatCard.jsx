import React from 'react';

export const StatCard = ({ title, value, subtitle, icon: Icon, color = 'var(--color-primary)' }) => {
  return (
    <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
      <div style={{
        width: '52px',
        height: '52px',
        borderRadius: 'var(--radius-md)',
        background: `${color}15`,
        color: color,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}>
        <Icon size={26} />
      </div>

      <div>
        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)' }}>{title}</div>
        <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', margin: '0.15rem 0' }}>{value}</div>
        {subtitle && <div style={{ fontSize: '0.75rem', color: 'var(--text-sub)' }}>{subtitle}</div>}
      </div>
    </div>
  );
};

export default StatCard;
