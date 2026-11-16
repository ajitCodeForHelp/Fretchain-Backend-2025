package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderProof;
import com.bt.fretchain.primary.pojo.enums.ProofStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FreightOrderProofRepository extends JpaRepository<FreightOrderProof, Long> {

    Optional<FreightOrderProof> findByFreightOrderAndStageAndDropSequence(FreightOrder order, ProofStage stage, Integer dropSequence);

    List<FreightOrderProof> findByFreightOrderOrderByDropSequenceAsc(FreightOrder order);
}
