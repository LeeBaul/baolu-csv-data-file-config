package com.libaolu.network.control.gui;

import com.libaolu.network.sampler.CheckNetworkPortSampler;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.*;

/**
 * @author libaolu
 * @date 2021/8/4 15:45
 */
public class CheckNetworkPortSamplerGui extends AbstractSamplerGui {

    /**
     * serverName or IP
     */
    private JTextField host;
    /**
     * port
     */
    private JTextField port;
    /**
     * timeout
     */
    private JTextField timeout;

    public CheckNetworkPortSamplerGui() {
        init();
        initFields();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), "North");
        VerticalPanel mainPanel = new VerticalPanel();
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createHostOption());
        optionsPanel.add(createPortOption());
        optionsPanel.add(createTimeoutOption());
        mainPanel.add(optionsPanel);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHostOption() {
        JLabel label = new JLabel("ServerName or IP");
        host = new JTextField(15);
        host.setMaximumSize(new Dimension(host.getPreferredSize()));
        label.setLabelFor(host);
        JPanel hostPanel = new JPanel(new FlowLayout());
        hostPanel.add(label);
        hostPanel.add(host);
        return hostPanel;
    }

    private JPanel createPortOption() {
        JLabel label = new JLabel("Port Number");
        port = new JTextField(14);
        port.setMaximumSize(new Dimension(port.getPreferredSize()));
        label.setLabelFor(port);
        JPanel portPanel = new JPanel(new FlowLayout());
        portPanel.add(label);
        portPanel.add(port);
        return portPanel;
    }

    private JPanel createTimeoutOption() {
        JLabel label = new JLabel("Connect(milliseconds)");
        timeout = new JTextField(10);
        timeout.setMaximumSize(new Dimension(timeout.getPreferredSize()));
        label.setLabelFor(timeout);
        JPanel timeoutPanel = new JPanel(new FlowLayout());
        timeoutPanel.add(label);
        timeoutPanel.add(timeout);
        return timeoutPanel;
    }

    public String getStaticLabel(){
        return "BaoLu NetworkPort Sampler";
    }

    @Override
    public String getLabelResource() {
        return super.getClass().getSimpleName();
    }

    @Override
    public TestElement createTestElement() {
        CheckNetworkPortSampler cnSampler = new CheckNetworkPortSampler();
        modifyTestElement(cnSampler);
        return cnSampler;
    }

    @Override
    public void modifyTestElement(TestElement testElement) {
        super.configureTestElement(testElement);
        testElement.setProperty(CheckNetworkPortSampler.HOST, host.getText(),"");
        testElement.setProperty(CheckNetworkPortSampler.PORT, port.getText(),"");
        testElement.setProperty(CheckNetworkPortSampler.CONNECT_TIME_OUT, timeout.getText(),"");
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        host.setText(element.getPropertyAsString(CheckNetworkPortSampler.HOST));
        port.setText(element.getPropertyAsString(CheckNetworkPortSampler.PORT));
        timeout.setText(element.getPropertyAsString(CheckNetworkPortSampler.CONNECT_TIME_OUT));
    }

    public void clearGui() {
        super.clearGui();
        initFields();
    }
    private void initFields(){
        host.setText("${ip}");
        port.setText("${port}");
        timeout.setText("5000");
    }
}
