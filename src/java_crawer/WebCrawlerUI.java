package java_crawer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//http://47.113.219.182/index.html

public class WebCrawlerUI {
    private JFrame frame;
    private JTextField urlField;
    private JTextArea contentArea;
    private List<String> sensitiveWords;
    private WebCrawler webCrawler;

    public WebCrawlerUI() {
        sensitiveWords = new ArrayList<>();
        webCrawler = new WebCrawler(10);  // 使用 10 个线程进行爬取
        initComponents();
    }

    private void initComponents() {
        frame = new JFrame("Web Crawler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // URL 输入面板
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.white);
        topPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JLabel urlLabel = new JLabel("Enter URL:");
        urlField = new JTextField(48);
        topPanel.add(urlLabel);
        topPanel.add(urlField);
        frame.add(topPanel, BorderLayout.NORTH);

        // 内容显示面板
        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBackground(Color.black);
        contentArea.setForeground(Color.green);
        JScrollPane scrollPane = new JScrollPane(contentArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.white);
        bottomPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        bottomPanel.setLayout(new FlowLayout());

        JButton startButton = new JButton("Start crawling");
        JButton stopButton = new JButton("End crawl");
        JButton showHtmlButton = new JButton("Display HTML content");
        JButton showTextButton = new JButton("Display text content");
        JButton importButton = new JButton("Import sensitive thesaurus");
        JButton modifyButton = new JButton("Modify the sensitive thesaurus");

        // 添加按钮事件监听
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 获取文本字段内容，并检查是否为空或只包含空白字符
                boolean isEmpty = urlField.getText().trim().isEmpty();
                if (isEmpty) {
                    // 如果为空，显示警告对话框
                    JOptionPane.showMessageDialog(frame, "请不要输入空值", "错误", JOptionPane.ERROR_MESSAGE);
                } else {
                    // 如果不为空，调用 startCrawling 方法并传递 URL
                    startCrawling(urlField.getText());
                }
            }
        });
        stopButton.addActionListener(e -> stopCrawling());
        showHtmlButton.addActionListener(e -> showHtmlContent());
        showTextButton.addActionListener(e -> showTextContent());
        importButton.addActionListener(e -> importSensitiveWords());
        modifyButton.addActionListener(e -> modifySensitiveWords());
        
        Box box = Box.createVerticalBox();//创建垂直box

        box.add(startButton);
        box.add(Box.createVerticalStrut(10));
        box.add(stopButton);
        box.add(Box.createVerticalStrut(10));
        box.add(showHtmlButton);
        box.add(Box.createVerticalStrut(10));
        box.add(showTextButton);
        box.add(Box.createVerticalStrut(10));
        box.add(importButton);
        box.add(Box.createVerticalStrut(10));
        box.add(modifyButton);
        
        bottomPanel.add(box);

        frame.add(bottomPanel, BorderLayout.EAST);

        frame.setVisible(true);
    }

    //开始爬取
    private void startCrawling(String url) {
    	contentArea.append("Start crawling: " + url + "\n");
    	webCrawler.startCrawling(url);
    }

    //结束爬取
    private void stopCrawling() {
        contentArea.append("End crawl\n");
        webCrawler.stopCrawling();
        Set<String> visitedUrls = webCrawler.getVisitedUrls();
        if (visitedUrls.isEmpty()) {
        	contentArea.append("crawled: nothing" + "\n");
        	return;
        }
        else {
        	for (String url : visitedUrls) {
        		contentArea.append("crawled: " + url + "\n");
            }
        }
    }

    //显示html内容
    private void showHtmlContent() {
        showUrlSelectionDialog("html");
    }

    //显示文本内容
    private void showTextContent() {
        showUrlSelectionDialog("text");
    }
    
    //将内容放在屏幕上显示
    private void showUrlSelectionDialog(String contentType) {
        Set<String> visitedUrls = webCrawler.getVisitedUrls();
        if (visitedUrls.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "没有已爬取的URL可供选择。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(frame, "选择URL", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);

        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String url : visitedUrls) {
            JCheckBox checkBox = new JCheckBox(url);
            checkBoxes.add(checkBox);
            checkBoxPanel.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton showButton = new JButton("显示内容");
        showButton.addActionListener(e -> {
            contentArea.setText("");
            for (JCheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    String url = checkBox.getText();
                    String content = contentType.equals("html") ? webCrawler.getHtmlContent(url) : webCrawler.getTextContent(url);
                    highlightSensitiveWords(content);
                }
            }
            dialog.dispose();
        });

        dialog.add(showButton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //高亮敏感词内容
    private void highlightSensitiveWords(String content) {
        contentArea.setText(content);
        Highlighter highlighter = contentArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        for (String word : sensitiveWords) {
            int index = content.indexOf(word);
            while (index >= 0) {
                try {
                    highlighter.addHighlight(index, index + word.length(), painter);
                    index = content.indexOf(word, index + word.length());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

  //敏感词
    private void importSensitiveWords() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadSensitiveWordsFromFile(selectedFile);
        }
    }

    //敏感词
    private void loadSensitiveWordsFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            sensitiveWords.clear();  // 清空已有的敏感词
            while ((line = reader.readLine()) != null) {
                sensitiveWords.add(line.trim());
                contentArea.setText("");
                contentArea.append("敏感词库导入如下: " + sensitiveWords + "\n");
            }
            System.out.println("敏感词库导入成功: " + sensitiveWords.size() + " 个敏感词");
        } catch (IOException e) {
            System.err.println("导入敏感词库失败: " + e.getMessage());
        }
    }

    //修改敏感词
    private void modifySensitiveWords() {
        // 创建一个新的窗口用于修改敏感词
        JFrame modifyFrame = new JFrame("修改敏感词库");
        modifyFrame.setSize(400, 300);
        modifyFrame.setLayout(new BorderLayout());

        JTextArea wordsArea = new JTextArea();
        wordsArea.setLineWrap(true);
        wordsArea.setWrapStyleWord(true);

        // 加载当前的敏感词到文本区域
        for (String word : sensitiveWords) {
            wordsArea.append(word + "\n");
        }

        JScrollPane scrollPane = new JScrollPane(wordsArea);
        modifyFrame.add(scrollPane, BorderLayout.CENTER);

        // 保存按钮用于保存修改
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveSensitiveWords(wordsArea.getText()));
        modifyFrame.add(saveButton, BorderLayout.SOUTH);

        modifyFrame.setVisible(true);
    }

    private void saveSensitiveWords(String wordsText) {
        sensitiveWords.clear();
        String[] wordsArray = wordsText.split("\\n");
        for (String word : wordsArray) {
            if (!word.trim().isEmpty()) {
                sensitiveWords.add(word.trim());
            }
        }
        System.out.println("敏感词库已更新: " + sensitiveWords.size() + " 个敏感词");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WebCrawlerUI::new);
    }
}