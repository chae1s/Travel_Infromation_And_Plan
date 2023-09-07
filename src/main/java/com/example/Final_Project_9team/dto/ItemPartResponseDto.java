package com.example.Final_Project_9team.dto;

import com.example.Final_Project_9team.entity.item.Item;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemPartResponseDto {
    private Long itemId;
    private String contentId;
    private String contentTypeId;
    private String title;
    private String firstImage;
    private String upmyundong;
    private String sido;
    private String siGunGu;
    private String fullAddress;

    public static ItemPartResponseDto fromEntity(Item item) {
        return ItemPartResponseDto.builder()
                .itemId(item.getId())
                .contentId(item.getContentId())
                .contentTypeId(item.getContentTypeId())
                .title(item.getName())
                .firstImage(item.getFirstImage())
                .upmyundong(item.getLocation().getUpmyundong())
                .sido(item.getLocation().getSido())
                .siGunGu(item.getLocation().getSigungu())
                .fullAddress(item.getLocation().getFullAddress())
                .build();
    }
}
