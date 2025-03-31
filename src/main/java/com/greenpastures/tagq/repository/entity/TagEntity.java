package com.greenpastures.tagq.repository.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Tag")
public class TagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private int tagId;

    @Size(max = 255, message = "태그 길이는 255자를 넘을 수 없습니다.")
    @Column(name = "tag_name", nullable = false)
    private String tag;

    @JsonBackReference
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private List<QuestionEntity> questions = new ArrayList<>();

    @Override
    public String toString() {
        return "TagEntity{" +
                "tagId=" + tagId +
                ", tagName='" + tag + '\'' +
                '}';
    }
}