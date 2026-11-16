package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.dto.ProofDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderAddress;
import com.bt.fretchain.primary.pojo.FreightOrderProof;
import com.bt.fretchain.primary.pojo.enums.AddressRole;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.ProofStage;
import com.bt.fretchain.primary.repository.FreightOrderAddressRepository;
import com.bt.fretchain.primary.repository.FreightOrderProofRepository;
import com.bt.fretchain.primary.repository.FreightOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Proof of pickup and delivery for freight orders: the goods check, photographs and the customer's e-signature.
 *
 * What is required (the standard for freight carriers):
 *  - at the pickup, before loading is finished: the goods match the order, the quantity is right, the visible
 *    condition is fine (or the differences / damage are written down), at least one photo, and the sender's signature;
 *  - at every drop, before unloading is finished: the same check on delivery, at least one photo, the receiver's signature.
 */
@Service
@RequiredArgsConstructor
public class ProofService {

    public static final int MIN_PHOTOS = 1;

    private final FreightOrderProofRepository proofs;
    private final FreightOrderRepository orders;
    private final FreightOrderAddressRepository addresses;
    private final OrderInfoBuilder info;

    // ---------------------------------------------------------------- driver

    @Transactional(readOnly = true)
    public ProofDTO.View get(Driver driver, String orderUuid, ProofStage stage, int dropSequence) {
        FreightOrder o = mine(driver, orderUuid);
        return proofs.findByFreightOrderAndStageAndDropSequence(o, stage, stage == ProofStage.PICKUP ? 0 : dropSequence)
                .map(p -> view(p, o)).orElse(blank(o, stage, dropSequence));
    }

    /** Saves what the driver sends; fields left out keep their value, so he can save photos first and sign later. */
    @Transactional
    public ProofDTO.View save(Driver driver, String orderUuid, ProofStage stage, int dropSequence, ProofDTO.Save req) {
        FreightOrder o = mine(driver, orderUuid);
        if (o.getOrderStatus() == OrderStatus.DELIVERED) throw new BadRequestException("driver.order.finished");
        int drops = drops(o).size();
        int seq = stage == ProofStage.PICKUP ? 0 : dropSequence;
        if (stage == ProofStage.DROP && (seq < 1 || seq > Math.max(drops, 1))) throw new BadRequestException("proof.drop.invalid");

        FreightOrderProof p = proofs.findByFreightOrderAndStageAndDropSequence(o, stage, seq).orElseGet(() -> {
            FreightOrderProof n = new FreightOrderProof();
            n.setFreightOrder(o);
            n.setStage(stage);
            n.setDropSequence(seq);
            n.setExpectedQuantity(info.expectedQuantity(o));
            return n;
        });
        p.setDriver(driver);
        if (req.getGoodsMatch() != null) p.setGoodsMatch(req.getGoodsMatch());
        if (req.getQuantityConfirmed() != null) p.setQuantityConfirmed(req.getQuantityConfirmed());
        if (req.getConditionOk() != null) p.setConditionOk(req.getConditionOk());
        if (req.getActualQuantity() != null) p.setActualQuantity(req.getActualQuantity().trim());
        if (req.getDiscrepancies() != null) p.setDiscrepancies(req.getDiscrepancies().trim());
        if (req.getPhotoUrls() != null) {
            List<String> clean = req.getPhotoUrls().stream().filter(Objects::nonNull).map(String::trim).filter(u -> !u.isEmpty()).collect(Collectors.toList());
            for (String u : clean) {
                if (!u.startsWith("https://") && !u.startsWith("http://")) throw new BadRequestException("proof.photo.invalid");
            }
            p.setPhotoUrls(String.join("\n", clean));
        }
        if (req.getSignatureSvg() != null) {
            String svg = req.getSignatureSvg().trim();
            if (svg.isEmpty()) {
                p.setSignatureSvg(null);
                p.setSignedAt(null);
            } else {
                String low = svg.toLowerCase();
                if (!low.startsWith("<svg") || low.contains("<script") || low.contains("onload") || low.contains("javascript:")) {
                    throw new BadRequestException("proof.signature.invalid");
                }
                p.setSignatureSvg(svg);
                p.setSignedAt(LocalDateTime.now());
            }
        }
        if (req.getSignerName() != null) p.setSignerName(req.getSignerName().trim());
        if (req.getLatitude() != null) p.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) p.setLongitude(req.getLongitude());
        p.setCapturedAt(LocalDateTime.now());
        proofs.save(p);
        return view(p, o);
    }

    /** Throws when the proof the next step needs is not complete. Called when the driver presses the button. */
    public void assertReady(FreightOrder o, DriverProgress.ProofNeed need) {
        if (need == null) return;
        FreightOrderProof p = proofs.findByFreightOrderAndStageAndDropSequence(o, need.stage(), need.dropSequence()).orElse(null);
        if (p == null || !isComplete(p)) {
            throw new BadRequestException(need.stage() == ProofStage.PICKUP ? "proof.pickup.required" : "proof.drop.required");
        }
    }

    /** What the driver still has to do at the step he is on, shown on the order screen. */
    @Transactional(readOnly = true)
    public OfferDTO.ProofRequirement requirement(FreightOrder o, DriverProgress.ProofNeed need) {
        if (need == null) return null;
        OfferDTO.ProofRequirement r = new OfferDTO.ProofRequirement();
        r.setStage(need.stage().name());
        r.setDropSequence(need.stage() == ProofStage.DROP ? need.dropSequence() : null);
        r.setNeedsChecklist(true);
        r.setMinPhotos(MIN_PHOTOS);
        r.setNeedsSignature(true);
        FreightOrderProof p = proofs.findByFreightOrderAndStageAndDropSequence(o, need.stage(), need.dropSequence()).orElse(null);
        r.setSaved(p == null ? blank(o, need.stage(), need.dropSequence()) : view(p, o));
        r.setComplete(p != null && isComplete(p));
        return r;
    }

    /** Every saved proof of an order, pickup first (admin / fleet owner / customer detail). */
    @Transactional(readOnly = true)
    public List<ProofDTO.View> forOrder(FreightOrder o) {
        List<ProofDTO.View> list = new ArrayList<>();
        for (FreightOrderProof p : proofs.findByFreightOrderOrderByDropSequenceAsc(o)) {
            list.add(view(p, o));
        }
        return list;
    }

    static boolean isComplete(FreightOrderProof p) {
        if (p.getGoodsMatch() == null || p.getQuantityConfirmed() == null || p.getConditionOk() == null) return false;
        boolean anyProblem = !p.getGoodsMatch() || !p.getQuantityConfirmed() || !p.getConditionOk();
        if (anyProblem && (p.getDiscrepancies() == null || p.getDiscrepancies().isBlank())) return false;
        if (photoList(p.getPhotoUrls()).size() < MIN_PHOTOS) return false;
        return p.getSignatureSvg() != null && !p.getSignatureSvg().isBlank()
                && p.getSignerName() != null && !p.getSignerName().isBlank();
    }

    // ---------------------------------------------------------------- helpers

    private FreightOrder mine(Driver driver, String orderUuid) {
        FreightOrder o = orders.findByUuidAndDeletedFalse(orderUuid).orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        if (o.getDriver() == null || !o.getDriver().getRecordId().equals(driver.getRecordId())) {
            throw new ResourceNotFoundException("record.notfound");
        }
        return o;
    }

    private List<FreightOrderAddress> drops(FreightOrder o) {
        return addresses.findByFreightOrderOrderBySequenceNoAsc(o).stream().filter(x -> x.getAddressRole() == AddressRole.DROP).collect(Collectors.toList());
    }

    private static List<String> photoList(String joined) {
        if (joined == null || joined.isBlank()) return new ArrayList<>();
        return Arrays.stream(joined.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private ProofDTO.View blank(FreightOrder o, ProofStage stage, int dropSequence) {
        ProofDTO.View v = new ProofDTO.View();
        v.setStage(stage.name());
        v.setDropSequence(stage == ProofStage.PICKUP ? 0 : dropSequence);
        v.setExpectedQuantity(info.expectedQuantity(o));
        v.setDropName(dropName(o, stage, dropSequence));
        return v;
    }

    private ProofDTO.View view(FreightOrderProof p, FreightOrder o) {
        ProofDTO.View v = new ProofDTO.View();
        v.setStage(p.getStage().name());
        v.setDropSequence(p.getDropSequence());
        v.setDropName(dropName(o, p.getStage(), p.getDropSequence()));
        v.setGoodsMatch(p.getGoodsMatch());
        v.setQuantityConfirmed(p.getQuantityConfirmed());
        v.setExpectedQuantity(p.getExpectedQuantity());
        v.setActualQuantity(p.getActualQuantity());
        v.setConditionOk(p.getConditionOk());
        v.setDiscrepancies(p.getDiscrepancies());
        v.setPhotoUrls(photoList(p.getPhotoUrls()));
        v.setSignatureSvg(p.getSignatureSvg());
        v.setSignerName(p.getSignerName());
        v.setSignedAt(p.getSignedAt());
        v.setCapturedAt(p.getCapturedAt());
        v.setDriverName(p.getDriver() == null ? null : p.getDriver().getFullName());
        v.setLatitude(p.getLatitude());
        v.setLongitude(p.getLongitude());
        v.setComplete(isComplete(p));
        return v;
    }

    private String dropName(FreightOrder o, ProofStage stage, int dropSequence) {
        if (stage == ProofStage.PICKUP) {
            return o.getPickupAddress() == null ? null : o.getPickupAddress().getAddressName();
        }
        List<FreightOrderAddress> d = drops(o);
        return dropSequence >= 1 && dropSequence <= d.size() ? d.get(dropSequence - 1).getAddressName() : null;
    }
}
