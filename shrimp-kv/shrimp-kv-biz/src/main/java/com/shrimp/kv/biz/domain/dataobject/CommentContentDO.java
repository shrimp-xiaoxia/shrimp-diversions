package com.shrimp.kv.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("comment_content")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentContentDO {
    @PrimaryKey
    private CommentContentPrimaryKey primaryKey;
    private String content;
}
