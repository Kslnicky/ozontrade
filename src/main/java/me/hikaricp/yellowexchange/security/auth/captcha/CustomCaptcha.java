package me.hikaricp.yellowexchange.security.auth.captcha;


import com.pig4cloud.captcha.base.Captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class CustomCaptcha extends Captcha {

    public CustomCaptcha() {
    }

    public CustomCaptcha(int width, int height) {
        this();
        this.setWidth(width);
        this.setHeight(height);
    }

    public CustomCaptcha(int width, int height, int len) {
        this(width, height);
        this.setLen(len);
    }

    public boolean out(OutputStream out) {
        return this.graphicsImage(this.textChar(), out);
    }

    public String toBase64() {
        return this.toBase64("data:image/png;base64,");
    }

    public String getContentType() {
        return "image/png";
    }

    private boolean graphicsImage(char[] strs, OutputStream out) {
        BufferedImage bi = null;

        try {
            bi = new BufferedImage(this.width, this.height, 1);
            Graphics2D g2d = (Graphics2D)bi.getGraphics();
            g2d.setColor(new Color(28, 28, 33));
            g2d.fillRect(0, 0, this.width, this.height);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //this.drawOval(2, g2d);
            g2d.setStroke(new BasicStroke(2.0F, 0, 2));
            //this.drawBesselLine(1, g2d);
            g2d.setFont(this.getFont());
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int fW = this.width / strs.length;
            int fSp = (fW - (int)fontMetrics.getStringBounds("W", g2d).getWidth()) / 2;

            for(int i = 0; i < strs.length; ++i) {
                g2d.setColor(this.color());
                int fY = this.height - (this.height - (int)fontMetrics.getStringBounds(String.valueOf(strs[i]), g2d).getHeight() >> 1);
                g2d.drawString(String.valueOf(strs[i]), i * fW + fSp + 3, fY - 3);
            }

            g2d.dispose();
            ImageIO.write(bi, "png", out);
            out.flush();
            return true;
        } catch (IOException var18) {
            var18.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException var17) {
                var17.printStackTrace();
            }

            if (bi != null) {
                bi.getGraphics().dispose();
            }

        }

        return false;
    }
}
