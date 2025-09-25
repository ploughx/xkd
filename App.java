package com.watermark;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;
// 需要在pom.xml中添加以下依赖:
// <dependency>
//     <groupId>org.apache.commons</groupId>
//     <artifactId>commons-imaging</artifactId>
//     <version>1.0-alpha2</version>
// </dependency>
// 检查是否已在pom.xml中添加commons-imaging依赖
// 如果仍无法解析,请确保Maven已正确下载依赖并刷新项目
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.*;
import org.apache.commons.imaging.formats.jpeg.exif.*;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 用户输入图片文件路径
        System.out.println("请输入图片文件路径:");
        String inputPath = scanner.nextLine();

        // 用户设置水印参数
        WatermarkConfig config = getUserConfig(scanner);

        File inputDir = new File(inputPath);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.out.println("输入的路径无效或不是目录。");
            return;
        }

        File outputDir = new File(inputDir.getParent(), inputDir.getName() + "_watermark");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        File[] files = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
        if (files == null || files.length == 0) {
            System.out.println("目录中没有找到图片文件。");
            return;
        }

        for (File file : files) {
            processImage(file, outputDir, config);
        }

        System.out.println("所有图片处理完成。");
    }

    private static WatermarkConfig getUserConfig(Scanner scanner) {
        System.out.println("请输入字体大小 (例如: 20):");
        int fontSize = scanner.nextInt();
        scanner.nextLine(); // 清除换行符

        System.out.println("请输入字体颜色 (例如: #FF0000):");
        String fontColor = scanner.nextLine();

        System.out.println("请输入水印位置 (left-top, center, right-bottom):");
        String position = scanner.nextLine();

        return new WatermarkConfig(fontSize, Color.decode(fontColor), position);
    }

    private static void processImage(File file, File outputDir, WatermarkConfig config) {
        try {
            String date = extractExifDate(file);
            if (date == null) {
                System.out.println("文件 " + file.getName() + " 没有拍摄时间信息。");
                return;
            }

            BufferedImage image = ImageIO.read(file);
            BufferedImage watermarkedImage = addWatermark(image, date, config);

            File outputFile = new File(outputDir, file.getName());
            ImageIO.write(watermarkedImage, "jpg", outputFile);

            System.out.println("已处理文件: " + file.getName());
        } catch (Exception e) {
            System.out.println("处理文件 " + file.getName() + " 时出错: " + e.getMessage());
        }
    }

    private static String extractExifDate(File file) {
        try {
            JpegImageMetadata metadata = (JpegImageMetadata) Imaging.getMetadata(file);
            if (metadata != null) {
                TiffField field = metadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);
                if (field != null) {
                    String dateTime = field.getStringValue();
                    return dateTime.split(" ")[0].replace(":", "-");
                }
            }
        } catch (ImageReadException | IOException e) {
            System.out.println("无法读取 EXIF 信息: " + e.getMessage());
        }
        return null;
    }

    private static BufferedImage addWatermark(BufferedImage image, String text, WatermarkConfig config) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) watermarked.getGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setFont(new Font("Arial", Font.BOLD, config.getFontSize()));
        g2d.setColor(config.getFontColor());

        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();

        int x = 0, y = 0;
        switch (config.getPosition().toLowerCase()) {
            case "left-top":
                x = 10;
                y = textHeight;
                break;
            case "center":
                x = (width - textWidth) / 2;
                y = (height - textHeight) / 2 + textHeight;
                break;
            case "right-bottom":
                x = width - textWidth - 10;
                y = height - 10;
                break;
            default:
                System.out.println("未知的位置参数，默认使用左上角。");
                x = 10;
                y = textHeight;
        }

        g2d.drawString(text, x, y);
        g2d.dispose();

        return watermarked;
    }
}

class WatermarkConfig {
    private final int fontSize;
    private final Color fontColor;
    private final String position;

    public WatermarkConfig(int fontSize, Color fontColor, String position) {
        this.fontSize = fontSize;
        this.fontColor = fontColor;
        this.position = position;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public String getPosition() {
        return position;
    }
}