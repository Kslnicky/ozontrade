package me.yukitale.yellowexchange.panel.common.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class CryptoLending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String coinSymbol;

    private double percent7days;

    private double percent14days;

    private double percent30days;

    private double percent90days;

    private double percent180days;

    private double percent360days;

    private double minAmount;

    private double maxAmount;

    public void setPercents(double percent7days, double percent14days, double percent30days, double percent90days, double percent180days, double percent360days) {
        this.percent7days = percent7days;
        this.percent14days = percent14days;
        this.percent30days = percent30days;
        this.percent90days = percent90days;
        this.percent180days = percent180days;
        this.percent360days = percent360days;
    }
}
