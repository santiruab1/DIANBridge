package xmlinvoiceviewer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class XMLInvoiceViewer {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> compradorComboBox;
    private List<String> archivosConErrores;

    public XMLInvoiceViewer() {
        // Crear la ventana principal
        frame = new JFrame("Lector de Facturas XML");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Inicializar la lista de archivos con errores
        archivosConErrores = new ArrayList<>();

        // Estilo visual de la ventana principal
        frame.getContentPane().setBackground(new Color(230, 230, 240)); // Fondo gris claro


        // Crear el modelo de tabla con columnas visibles y ocultas
        String[] columnNames = {"Proveedor", "Número de Factura", "Fecha", "Fecha de Vencimiento", "Nombre del Item", "Cantidad", "Total en Pesos", "Centro de Costos", "Valor", "IVA"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Hacer que las columnas "Valor" e "IVA" no sean visibles en la interfaz gráfica
                return column < 8;
            }
        };
        table = new JTable(tableModel);

        // Ocultar las columnas "Valor" e "IVA" en la interfaz gráfica
        table.removeColumn(table.getColumnModel().getColumn(8));
        table.removeColumn(table.getColumnModel().getColumn(8));

        // Estilo de la tabla
        table.setBackground(new Color(240, 245, 250)); // Fondo azul muy claro
        table.setForeground(Color.DARK_GRAY); // Texto gris oscuro
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setBackground(new Color(100, 130, 180)); // Azul medio
        table.getTableHeader().setForeground(Color.WHITE); // Texto blanco en encabezado
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        // Configurar la columna "Centro de Costos" para usar JComboBox como editor de celdas
        TableColumn centroCostosColumn = table.getColumnModel().getColumn(7);
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"Administración", "Deposito", "Parqueadero", "SZU-505", "STA-068", "STE-436", "STE-456", "STE-421", "TTZ-648", "WCP-392", "UIC-841", "TNH-287", "SZV-209", "GDX-212"});
        centroCostosColumn.setCellEditor(new DefaultCellEditor(comboBox));

        // Agregar la tabla a un scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(240, 245, 250)); // Fondo azul muy claro dentro del scroll pane
        scrollPane.getViewport().setBackground(new Color(240, 245, 250)); // Fondo azul muy claro dentro del scroll pane
        frame.add(scrollPane, BorderLayout.CENTER);

        // Crear el selector de comprador
        JPanel compradorPanel = new JPanel();
        compradorPanel.setBackground(new Color(204, 255, 204)); // Fondo verde claro
        compradorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        compradorPanel.add(new JLabel("Comprador: "));
        compradorComboBox = new JComboBox<>(new String[]{"LEONARDO ANTONIO GONZALEZ CARMONA", "TRANSPORTES Y VOLQUETAS GONZALEZ SAS", "GRUPO NUTABE SAS"}); // Agregar más compradores aquí
        compradorComboBox.setFont(new Font("Arial", Font.BOLD, 14));
        compradorComboBox.setBackground(new Color(204, 255, 204)); // Fondo verde claro
        compradorComboBox.setForeground(Color.BLACK); // Texto negro
        compradorPanel.add(compradorComboBox);
        frame.add(compradorPanel, BorderLayout.NORTH);

        // Crear botones para seleccionar archivo o carpeta
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(120, 140, 160)); // Azul gris oscuro para el fondo
        JButton selectFileButton = new JButton("Seleccionar Archivo XML");
        JButton selectFolderButton = new JButton("Seleccionar Carpeta");
        JButton exportToExcelButton = new JButton("Exportar a Excel");
        JButton verErroresButton = new JButton("Ver Archivos con Errores");

        // Estilo de los botones
        selectFileButton.setBackground(new Color(255, 140, 0)); // Naranja claro
        selectFileButton.setForeground(Color.BLACK); // Texto negro
        selectFileButton.setFont(new Font("Arial", Font.BOLD, 14));

        selectFolderButton.setBackground(new Color(255, 140, 0)); // Naranja claro
        selectFolderButton.setForeground(Color.BLACK); // Texto negro
        selectFolderButton.setFont(new Font("Arial", Font.BOLD, 14));

        exportToExcelButton.setBackground(new Color(255, 140, 0)); // Naranja claro
        exportToExcelButton.setForeground(Color.WHITE); // Texto blanco
        exportToExcelButton.setFont(new Font("Arial", Font.BOLD, 14));

        verErroresButton.setBackground(new Color(255, 140, 0)); // Naranja claro
        verErroresButton.setForeground(Color.BLACK); // Texto negro
        verErroresButton.setFont(new Font("Arial", Font.BOLD, 14));

        buttonPanel.add(selectFileButton);
        buttonPanel.add(selectFolderButton);
        buttonPanel.add(exportToExcelButton);
        buttonPanel.add(verErroresButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Acción para seleccionar un archivo XML
        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    procesarArchivoXML(selectedFile);
                }
            }
        });

        // Acción para seleccionar una carpeta que contiene archivos XML
        selectFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser folderChooser = new JFileChooser();
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int option = folderChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = folderChooser.getSelectedFile();
                    for (File file : selectedFolder.listFiles()) {
                        if (file.getName().endsWith(".xml")) {
                            procesarArchivoXML(file);
                        }
                    }
                }
            }
        });

        // Acción para exportar los datos de la tabla a un archivo Excel
        exportToExcelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser folderChooser = new JFileChooser();
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int option = folderChooser.showSaveDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = folderChooser.getSelectedFile();
                    exportarTablaAExcel(selectedFolder);
                }
            }
        });

        // Acción para ver los archivos con errores
        verErroresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (archivosConErrores.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "No se encontraron errores en los archivos procesados.", "Archivos con Errores", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    StringBuilder errorMessage = new StringBuilder("Archivos con errores:\n");
                    for (String archivo : archivosConErrores) {
                        errorMessage.append(archivo).append("\n");
                    }
                    JOptionPane.showMessageDialog(frame, errorMessage.toString(), "Archivos con Errores", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Mostrar la ventana principal
        frame.setVisible(true);
    }

    private void procesarArchivoXML(File xmlFile) {
        try {
            // Configurar el parser para leer el archivo XML
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true); // Hacer que el parser sea consciente de los espacios de nombres
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();

            // Obtener proveedor, número de factura, prefijo, fechas usando XPath
            String proveedor = (String) xPath.evaluate("//*[local-name()='PartyTaxScheme']/*[local-name()='RegistrationName']", doc, XPathConstants.STRING);
            String prefijo = (String) xPath.evaluate("//*[local-name()='Prefix']", doc, XPathConstants.STRING);
            String idFactura = (String) xPath.evaluate("//*[local-name()='ID']", doc, XPathConstants.STRING);
            String fechaEmision = (String) xPath.evaluate("//*[local-name()='IssueDate']", doc, XPathConstants.STRING);
            String fechaVencimiento = (String) xPath.evaluate("//*[local-name()='DueDate']", doc, XPathConstants.STRING);
            String nit = (String) xPath.evaluate("//*[local-name()='AccountingSupplierParty']/*[local-name()='Party']/*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']", doc, XPathConstants.STRING);
            if (nit != null) {
                nit = nit.trim(); // Asegurar que el NIT se extrae correctamente
            }

            // Obtener los ítems de la factura
            NodeList items = (NodeList) xPath.evaluate("//*[local-name()='InvoiceLine']", doc, XPathConstants.NODESET);
            String comprador = (String) compradorComboBox.getSelectedItem(); // Obtener el comprador seleccionado
            for (int i = 0; i < items.getLength(); i++) {
                Node itemNode = items.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;
                    String nombreItem = (String) xPath.evaluate(".//*[local-name()='Description']", itemElement, XPathConstants.STRING);
                    String cantidad = (String) xPath.evaluate(".//*[local-name()='InvoicedQuantity']", itemElement, XPathConstants.STRING);
                    String valor = (String) xPath.evaluate(".//*[local-name()='LineExtensionAmount']", itemElement, XPathConstants.STRING);
                    String iva = (String) xPath.evaluate(".//*[local-name()='TaxAmount']", itemElement, XPathConstants.STRING);

                    // Convertir valores a formato pesos (usando puntos como separadores decimales y formatear)
                    double valorDouble = valor.isEmpty() ? 0.0 : Double.parseDouble(valor.replace(",", "."));
                    double ivaDouble = iva.isEmpty() ? 0.0 : Double.parseDouble(iva.replace(",", "."));
                    double totalDouble = valorDouble + ivaDouble;
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
                    String totalEnPesos = currencyFormat.format(totalDouble);

                    // Agregar los datos a la tabla, incluyendo "Valor" e "IVA" (aunque no se muestren en la interfaz)
                    tableModel.addRow(new Object[]{proveedor, idFactura, fechaEmision, fechaVencimiento, nombreItem, cantidad, totalEnPesos, "Administración", valor, iva});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            archivosConErrores.add(xmlFile.getName()); // Agregar archivo a la lista de errores
            JOptionPane.showMessageDialog(frame, "Error al procesar el archivo: " + xmlFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarTablaAExcel(File directory) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Facturas");

        // Crear la fila de encabezado
        Row headerRow = sheet.createRow(0);
        String[] exportColumns = {"NIT", "Comprador", "Proveedor", "Número de Factura", "Fecha", "Fecha de Vencimiento", "Nombre del Item", "Cantidad", "Valor", "IVA", "Centro de Costos"};
        for (int i = 0; i < exportColumns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(exportColumns[i]);
        }

        // Rellenar las filas con los datos de la tabla
        String comprador = (String) compradorComboBox.getSelectedItem(); // Obtener el comprador seleccionado
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Row row = sheet.createRow(i + 1);
            int colIndex = 0;

            // Agregar la columna de NIT al principio
            String nit = (String) tableModel.getValueAt(i, 7); // Obtener el NIT desde la tabla
            Cell nitCell = row.createCell(colIndex++);
            nitCell.setCellValue(nit);

            // Agregar la columna de comprador
            Cell compradorCell = row.createCell(colIndex++);
            compradorCell.setCellValue(comprador);

            // Obtener datos de la tabla y agregar valores e IVA para exportar
            String proveedor = (String) tableModel.getValueAt(i, 0);
            String idFactura = (String) tableModel.getValueAt(i, 1);
            String fecha = (String) tableModel.getValueAt(i, 2);
            String fechaVencimiento = (String) tableModel.getValueAt(i, 3);
            String nombreItem = (String) tableModel.getValueAt(i, 4);
            String cantidad = (String) tableModel.getValueAt(i, 5);
            String valor = (String) tableModel.getValueAt(i, 9);
            String iva = (String) tableModel.getValueAt(i, 9);
            String centroCostos = (String) tableModel.getValueAt(i, 7);

            // Agregar datos a las celdas del Excel
            row.createCell(colIndex++).setCellValue(proveedor);
            row.createCell(colIndex++).setCellValue(idFactura);
            row.createCell(colIndex++).setCellValue(fecha);
            row.createCell(colIndex++).setCellValue(fechaVencimiento);
            row.createCell(colIndex++).setCellValue(nombreItem);
            row.createCell(colIndex++).setCellValue(cantidad);
            row.createCell(colIndex++).setCellValue(valor);
            row.createCell(colIndex++).setCellValue(iva);
            row.createCell(colIndex++).setCellValue(centroCostos);
        }

        // Guardar el archivo Excel en la carpeta seleccionada
        try {
            FileOutputStream fileOut = new FileOutputStream(new File(directory, "facturas.xlsx"));
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            JOptionPane.showMessageDialog(frame, "Exportado a Excel exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error al exportar a Excel", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new XMLInvoiceViewer());
    }
}
