package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.ProofStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * What the driver recorded at the pickup (goods check, photos, sender's e-signature) or at one drop
 * (condition on delivery, photos, receiver's e-signature). One row per order and stage; drop rows are numbered
 * from 1, the pickup row uses 0.
 */
@Entity
@Table(name = "freight_order_proofs",
        uniqueConstraints = @UniqueConstraint(name = "uk_freight_proof_stage", columnNames = {"freight_order_id", "stage", "drop_sequence"}))
@Getter
@Setter
public class FreightOrderProof extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freight_order_id", nullable = false)
    private FreightOrder freightOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProofStage stage;

    /** 0 for the pickup, 1.. for the drops. */
    @Column(name = "drop_sequence", nullable = false)
    private Integer dropSequence = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    /** Goods match the order description. */
    @Column(name = "goods_match")
    private Boolean goodsMatch;

    /** Package / cargo quantity matches. */
    @Column(name = "quantity_confirmed")
    private Boolean quantityConfirmed;

    /** What the order says the quantity is, and what the driver counted. */
    @Column(name = "expected_quantity", length = 200)
    private String expectedQuantity;

    @Column(name = "actual_quantity", length = 200)
    private String actualQuantity;

    /** Visible condition is fine (no damage). */
    @Column(name = "condition_ok")
    private Boolean conditionOk;

    /** Missing / extra items, damage, anything that differs from the order. */
    @Column(columnDefinition = "TEXT")
    private String discrepancies;

    /** Photo links, one per line. */
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    /** The customer's signature drawn on the phone, as SVG markup. */
    @Lob
    @Column(name = "signature_svg", columnDefinition = "LONGTEXT")
    private String signatureSvg;

    @Column(name = "signer_name", length = 150)
    private String signerName;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    private Double latitude;

    private Double longitude;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
}
