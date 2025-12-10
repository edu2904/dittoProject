package org.example;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.Gateways.Temporary.TaskGateway.TaskGateway;
import org.example.process.RoutePlanner;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
// class to create the Excel files for evaluation
public class ExcelWriter {


    public void exportRoutesToExcel(List<RoutePlanner.Route> routes,
                                    Path output){

        try (Workbook workbook = new XSSFWorkbook()){
            Sheet sheet = workbook.createSheet("Routes");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("RouteId");
            header.createCell(1).setCellValue("SegmentIndex");
            header.createCell(2).setCellValue("FromWarehouse");
            header.createCell(3).setCellValue("ToWarehouse");
            header.createCell(4).setCellValue("TaskType");
            header.createCell(5).setCellValue("Quantity");
            header.createCell(6).setCellValue("AssignedTrucks");
            header.createCell(7).setCellValue("Events");
            header.createCell(8).setCellValue("TaskTimeMin");
            header.createCell(9).setCellValue("RouteTimeMin");
            header.createCell(10).setCellValue("ReservedTrucks");



            int rowIndex = 1;

            for(RoutePlanner.Route route: routes) {
                String routeId = route.getRouteId();
                List<RoutePlanner.Segment> segments = route.getSegments();

                List<String> trucks = route.getExecutor();
                String truckAsString = (trucks == null || trucks.isEmpty()) ? "" : String.join(", ", trucks);

                List<String> events = route.getRouteEvents();
                String eventsAsString = (events == null || events.isEmpty()) ? "" : String.join(", ", events);
                double routeTimes = 0.0;
                List<Double> time = route.getTotalTimeMinutes();
                String timeAsString = (time == null || time.isEmpty()) ? "" : String.join(", ", time.toString());

                double averageTime = 0.0;
                if(time != null) {
                    averageTime = time.stream().mapToDouble(Double::doubleValue).sum();
                }

                for (int i = 0; i < segments.size(); i++) {
                    RoutePlanner.Segment segment = segments.get(i);
                    Row row = sheet.createRow(rowIndex++);


                    row.createCell(0).setCellValue(routeId);

                    row.createCell(1).setCellValue(i);

                    row.createCell(2).setCellValue(
                            segment.getFrom() != null ? segment.getFrom().getThingId() : "");
                    row.createCell(3).setCellValue(
                            segment.getTo() != null ? segment.getTo().getThingId() : "");

                    row.createCell(4).setCellValue(
                            segment.getTaskType() != null ? segment.getTaskType().name() : "");

                    row.createCell(5).setCellValue(segment.getQuantity());

                    row.createCell(6).setCellValue(truckAsString);
                    row.createCell(7).setCellValue(eventsAsString);

                    row.createCell(8).setCellValue(timeAsString);
                    row.createCell(9).setCellValue(averageTime);

                }
            }
            for(int col = 0; col <=7; col++){
                sheet.autoSizeColumn(col);
            }
            try(OutputStream os = Files.newOutputStream(output)) {
                workbook.write(os);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
