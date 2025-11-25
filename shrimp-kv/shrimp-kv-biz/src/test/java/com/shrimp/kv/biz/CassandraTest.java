package com.shrimp.kv.biz;

import com.shrimp.kv.biz.domain.dataobject.NoteContentDO;
import com.shrimp.kv.biz.domain.repository.NoteContentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Slf4j
public class CassandraTest {
    @Resource
    private NoteContentRepository noteContentRepository;

    @Test
    public void saveNoteContent() {
        NoteContentDO noteContentDO = NoteContentDO.builder()
                .id(UUID.randomUUID())
                .content("代码测试记录")
                .build();
        noteContentRepository.save(noteContentDO);
    }

    @Test
    public void selectNoteContent() {

        Optional<NoteContentDO> single = noteContentRepository.findById(UUID.fromString("887402d2-54b6-4845-b114-9787e36573cb"));
        single.ifPresent(System.out::println);
    }

    @Test
    public void updateNoteContent() {
        NoteContentDO noteContentDO = NoteContentDO.builder()
                .id(UUID.fromString("846b9368-9742-4bb1-93ba-611effd6a7d0"))
                .content("代码更新测试记录")
                .build();
        noteContentRepository.save(noteContentDO);
    }

    @Test
    public void deleteNoteContent() {
//        noteContentRepository.deleteById(UUID.fromString("846b9368-9742-4bb1-93ba-611effd6a7d0"));
        noteContentRepository.deleteAll();
    }
}
