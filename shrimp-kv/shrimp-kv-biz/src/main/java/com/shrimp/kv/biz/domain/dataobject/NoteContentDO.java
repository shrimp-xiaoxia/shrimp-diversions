package com.shrimp.kv.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * @author ccb
 */
@Table("note_content")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NoteContentDO {
    @PrimaryKey("id")
    private UUID id;
    private String content;
}
