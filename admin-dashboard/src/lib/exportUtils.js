import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

/**
 * AttendX Export Utilities
 * PDF styling matches the backend PDFReport class (Material 3 Institutional Theme)
 */

// Material 3 Palette (matching backend PDFReport)
const COLORS = {
  primary: [21, 101, 192],       // Deep Blue
  secondary: [66, 66, 66],       // Dark Gray
  accent: [13, 71, 161],         // Darker Accent
  surfaceVariant: [232, 240, 254], // Light Blue Surface
  error: [184, 27, 27],          // M3 Error Red
  success: [46, 125, 50],        // M3 Success Green
  gold: [255, 215, 0],           // Brand Gold
  white: [255, 255, 255],
  lightText: [220, 230, 255],
  grayText: [150, 150, 150],
  darkText: [33, 33, 33],
  divider: [230, 230, 230],
  altRow: [242, 247, 251],
  cardBg: [252, 252, 252],
};

// Logo cache — loaded once and reused
let logoCache = null;
let logoLoading = false;

async function loadLogo() {
  if (logoCache) return logoCache;
  if (logoLoading) return null;
  logoLoading = true;
  try {
    const resp = await fetch('/vjti.jpg');
    if (!resp.ok) return null;
    const blob = await resp.blob();
    return await new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => { logoCache = reader.result; resolve(logoCache); };
      reader.readAsDataURL(blob);
    });
  } catch { return null; }
  finally { logoLoading = false; }
}

// Pre-load logo on module init
loadLogo();

function formatNow() {
  return new Date().toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/**
 * Institutional Header — matches backend PDFReport.header()
 * Deep blue banner + gold accent + VJTI branding + AttendX system label + Logo
 */
function addHeader(doc, title, subtitle, filters) {
  const pw = doc.internal.pageSize.getWidth();

  // ── Grand Institutional Header Bar (Deep Blue) ──
  doc.setFillColor(...COLORS.primary);
  doc.rect(0, 0, pw, 38, 'F');

  // ── Gold Accent Line ──
  doc.setFillColor(...COLORS.gold);
  doc.rect(0, 38, pw, 1.5, 'F');

  // ── Institutional Logo (Right aligned, matching backend) ──
  if (logoCache) {
    try {
      // White background box for logo
      doc.setFillColor(255, 255, 255);
      doc.roundedRect(pw - 36, 5, 28, 28, 2, 2, 'F');
      doc.addImage(logoCache, 'JPEG', pw - 35, 6, 26, 26);
    } catch (e) {
      console.log('Logo embed skipped:', e.message);
    }
  }

  // ── Institution Name (White on Blue) ──
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.setTextColor(...COLORS.white);
  doc.text('VEERMATA JIJABAI TECHNOLOGICAL INSTITUTE', 12, 13);

  // ── Subtitle Line 1 ──
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(...COLORS.lightText);
  doc.text('ACADEMIC ATTENDANCE SYSTEM | SECURE BIOMETRIC LOGGING', 12, 19);

  // ── Subtitle Line 2 ──
  doc.text('MATUNGA, MUMBAI | ESTD. 1887', 12, 24);

  // ── AttendX Branding (bottom of header) ──
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(...COLORS.surfaceVariant);
  doc.text('ATTENDX | SECURE INTELLIGENT SYSTEM', 12, 34);

  // ── Date stamp (right side, below logo) ──
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7);
  doc.setTextColor(...COLORS.lightText);
  doc.text(`Generated: ${formatNow()}`, pw - 40, 34, { align: 'right' });

  // ── Report Title (below header) ──
  let startY = 48;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.setTextColor(...COLORS.primary);
  doc.text(` ${title.toUpperCase()}`, 10, startY);

  // ── M3 Title underline (accent thick + thin gray) ──
  startY += 3;
  doc.setDrawColor(...COLORS.primary);
  doc.setLineWidth(0.8);
  doc.line(10, startY, 55, startY);
  doc.setDrawColor(...COLORS.divider);
  doc.setLineWidth(0.2);
  doc.line(55, startY, pw - 10, startY);
  startY += 5;

  // ── Subtitle ──
  if (subtitle) {
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8.5);
    doc.setTextColor(...COLORS.secondary);
    doc.text(subtitle, 12, startY);
    startY += 5;
  }

  // ── Filter Badges ──
  if (filters && filters.length > 0) {
    const activeFilters = filters.filter(f => f.value);
    if (activeFilters.length > 0) {
      doc.setFontSize(7.5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(...COLORS.grayText);
      doc.text('Filters:', 12, startY);
      let x = 30;
      activeFilters.forEach(f => {
        doc.setFillColor(...COLORS.surfaceVariant);
        const label = `${f.label}: ${f.value}`;
        const w = doc.getTextWidth(label) + 8;
        doc.roundedRect(x, startY - 3.5, w, 6, 1.5, 1.5, 'F');
        doc.setTextColor(...COLORS.primary);
        doc.text(label, x + 4, startY);
        x += w + 3;
      });
      startY += 7;
    }
  }

  startY += 2;
  return startY;
}

/**
 * Institutional Footer — matches backend PDFReport.footer()
 * Gray divider + legal text + page number with VJTI branding
 */
function addFooter(doc) {
  const pages = doc.internal.getNumberOfPages();
  const pw = doc.internal.pageSize.getWidth();
  const ph = doc.internal.pageSize.getHeight();

  for (let i = 1; i <= pages; i++) {
    doc.setPage(i);

    // ── Divider line ──
    doc.setDrawColor(...COLORS.divider);
    doc.setLineWidth(0.3);
    doc.line(10, ph - 22, pw - 10, ph - 22);

    // ── Legal text ──
    doc.setFont('helvetica', 'italic');
    doc.setFontSize(7);
    doc.setTextColor(...COLORS.grayText);
    doc.text('Officially Authenticated Digital Academic Record. Confidentiality Governed by IT Act 2000.', pw / 2, ph - 16, { align: 'center' });

    // ── Page number + branding ──
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(7);
    doc.setTextColor(100, 100, 100);
    doc.text(`Page ${i} of ${pages} | VJTI-ACADEMIC-VERIFIED | OFFICIAL RECORD`, pw / 2, ph - 10, { align: 'center' });

    // ── Gold bottom accent ──
    doc.setFillColor(...COLORS.gold);
    doc.rect(0, ph - 2, pw, 2, 'F');
  }
}

/**
 * Export data as a beautifully formatted PDF matching the app's institutional style
 */
export function exportPDF({ title, subtitle, headers, rows, filters, filename }) {
  const doc = new jsPDF('landscape', 'mm', 'a4');
  const startY = addHeader(doc, title, subtitle, filters);

  autoTable(doc, {
    head: [headers],
    body: rows,
    startY,
    margin: { left: 10, right: 10, bottom: 28 },
    styles: {
      fontSize: 8.5,
      cellPadding: 4,
      lineColor: COLORS.divider,
      lineWidth: 0.2,
      textColor: COLORS.darkText,
      font: 'helvetica',
    },
    headStyles: {
      fillColor: COLORS.primary,
      textColor: COLORS.white,
      fontSize: 8,
      fontStyle: 'bold',
      halign: 'center',
    },
    alternateRowStyles: {
      fillColor: COLORS.altRow,
    },
    didDrawPage: (data) => {
      if (data.pageNumber > 1) {
        // Minimal header on continuation pages
        doc.setFillColor(...COLORS.primary);
        doc.rect(0, 0, doc.internal.pageSize.getWidth(), 10, 'F');
        doc.setFillColor(...COLORS.gold);
        doc.rect(0, 10, doc.internal.pageSize.getWidth(), 1, 'F');
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(8);
        doc.setTextColor(...COLORS.white);
        doc.text(`ATTENDX | ${title.toUpperCase()} (continued)`, 12, 7);
        doc.setTextColor(...COLORS.lightText);
        doc.text(`Page ${data.pageNumber}`, doc.internal.pageSize.getWidth() - 12, 7, { align: 'right' });
      }
    },
  });

  // ── Summary row below table ──
  const finalY = (doc.lastAutoTable?.finalY || doc.previousAutoTable?.finalY || startY + 20) + 5;
  doc.setFontSize(8);
  doc.setTextColor(...COLORS.secondary);
  doc.text(`Total Records: ${rows.length}`, 12, finalY);
  doc.setTextColor(...COLORS.grayText);
  doc.text(`Report generated by AttendX Admin Dashboard`, doc.internal.pageSize.getWidth() - 12, finalY, { align: 'right' });

  addFooter(doc);
  doc.save(filename || `${title.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0, 10)}.pdf`);
}

/**
 * Export data as CSV with BOM for Excel compatibility
 */
export function exportCSV({ headers, rows, filename }) {
  const csv = [headers, ...rows]
    .map(r => r.map(c => `"${String(c || '').replace(/"/g, '""')}"`).join(','))
    .join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename || 'export.csv';
  a.click();
}
