package gui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Simple bar chart drawn with Graphics2D (extra feature, still Swing only). */
public class RevenueChart extends JPanel {

    private Map<String, BigDecimal> data = new LinkedHashMap<>();
    private String title = "";

    public RevenueChart() {
        setBackground(UiStyle.PANEL);
        setPreferredSize(new Dimension(0, 200));
    }

    public void setData(String title, Map<String, BigDecimal> data) {
        this.title = title;
        this.data = data == null ? new LinkedHashMap<>() : data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int left = 60, right = 20, top = 34, bottom = 30;
        int plotW = w - left - right;
        int plotH = h - top - bottom;

        g2.setFont(UiStyle.HEADING_FONT);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(title, left, 22);

        double max = 0;
        for (BigDecimal v : data.values()) {
            max = Math.max(max, v.doubleValue());
        }
        g2.setFont(UiStyle.BODY_FONT.deriveFont(11f));
        FontMetrics fm = g2.getFontMetrics();

        if (data.isEmpty() || max == 0) {
            g2.setColor(UiStyle.MUTED);
            String msg = "No paid revenue in this period";
            g2.drawString(msg, left + (plotW - fm.stringWidth(msg)) / 2, top + plotH / 2);
            g2.dispose();
            return;
        }

        // gridlines + axis labels
        double niceMax = Math.ceil(max / 100.0) * 100;
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            int y = top + plotH - (int) (plotH * i / 4.0);
            g2.setColor(new Color(0xEAE6E0));
            g2.drawLine(left, y, left + plotW, y);
            g2.setColor(UiStyle.MUTED);
            String label = String.format("%.0f", niceMax * i / 4);
            g2.drawString(label, left - 8 - fm.stringWidth(label), y + 4);
        }

        // bars
        int n = data.size();
        double slot = plotW / (double) n;
        int barW = (int) Math.max(4, Math.min(48, slot * 0.6));
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : data.entrySet()) {
            double v = e.getValue().doubleValue();
            int barH = (int) (plotH * v / niceMax);
            int x = left + (int) (slot * i + (slot - barW) / 2);
            int y = top + plotH - barH;
            g2.setColor(UiStyle.PRIMARY);
            g2.fillRect(x, y, barW, barH);
            g2.setColor(Color.DARK_GRAY);
            String lbl = e.getKey();
            g2.drawString(lbl, x + (barW - fm.stringWidth(lbl)) / 2, top + plotH + 16);
            if (v > 0) {
                String val = String.format("%.0f", v);
                g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y - 4);
            }
            i++;
        }
        g2.dispose();
    }
}
