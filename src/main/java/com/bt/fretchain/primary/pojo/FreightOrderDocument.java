package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.FreightOrderDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "freight_order_documents")
@Getter
@Setter
public class FreightOrderDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false)
    private FreightOrder freightOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 100)
    private FreightOrderDocumentType documentType;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;
}