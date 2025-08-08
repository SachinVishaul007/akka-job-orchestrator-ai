package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.joborchestratorai.akkajoborchestratorai.models.ResumeData;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileReaderActor extends AbstractBehavior<MasterActor.ProcessExcelFile> {
    private final ActorRef<Object> storageActor;

    public static Behavior<MasterActor.ProcessExcelFile> create(ActorRef<Object> storageActor) {
        return Behaviors.setup(context -> new FileReaderActor(context, storageActor));
    }

    private FileReaderActor(ActorContext<MasterActor.ProcessExcelFile> context, ActorRef<Object> storageActor) {
        super(context);
        this.storageActor = storageActor;
    }

    @Override
    public Receive<MasterActor.ProcessExcelFile> createReceive() {
        return newReceiveBuilder()
                .onMessage(MasterActor.ProcessExcelFile.class, this::onProcessExcelFile)
                .build();
    }

    private Behavior<MasterActor.ProcessExcelFile> onProcessExcelFile(MasterActor.ProcessExcelFile msg) {
        try (FileInputStream fis = new FileInputStream(msg.filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<String> resumePoints = new ArrayList<>();

            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String content = cell.getStringCellValue().trim();
                    if (!content.isEmpty()) {
                        resumePoints.add(content);
                    }
                }
            }

            ResumeData resumeData = new ResumeData(UUID.randomUUID().toString(), msg.filePath, resumePoints);
            storageActor.tell(new MasterActor.StoreResumeData(resumeData));

            getContext().getLog().info("Processed {} resume points", resumePoints.size());
        } catch (Exception e) {
            getContext().getLog().error("Error processing file", e);
        }

        return this;
    }
}