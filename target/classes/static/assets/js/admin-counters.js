document.addEventListener('DOMContentLoaded', function() {
    let depositsCount = $("#deposits_unviewed").text().trim() === '' ? 0 : parseInt($("#deposits_unviewed").text().trim());
    let withdrawsCount = $("#withdrawals_unviewed").text().trim() === '' ? 0 : parseInt($("#withdrawals_unviewed").text().trim());
    let supportCount = $("#support_unviewed").text().trim() === '' ? 0 : parseInt($("#support_unviewed").text().trim());
    let kycCount = $("#kyc_unviewed").text().trim() === '' ? 0 : parseInt($("#kyc_unviewed").text().trim());

    setInterval(() => {
        $.ajax({
            url: "/api/admin/counters",
            type: "POST",
            contentType: 'application/json; charset=UTF-8',
            data: JSON.stringify({
                action: "GET_COUNTERS"
            }),
            success: function (response) {
                try {
                    const json = JSON.parse(response);

                    const countDeposits = parseInt(json['deposits_unviewed']);
                    const countWithdrawals = parseInt(json['withdrawals_unviewed']);
                    const countSupport = parseInt(json['support_unviewed']);
                    const countKyc = parseInt(json['kyc_unviewed']);

                    let playSound = false;
                    if (countDeposits > depositsCount) {
                        noti(getMessage('notifications.deposit.received'), 'success');
                        playSound = true;
                    }

                    if (countWithdrawals > withdrawsCount) {
                        noti(getMessage('notifications.withdrawal.received'), 'success');
                        playSound = true;
                    }

                    if (countSupport > supportCount) {
                        noti(getMessage('notifications.support.received'), 'success');
                        playSound = true;
                    }

                    if (countKyc > kycCount) {
                        noti(getMessage('notifications.kyc.received'), 'success');
                        playSound = true;
                    }

                    depositsCount = countDeposits;
                    withdrawsCount = countWithdrawals;
                    supportCount = countSupport;
                    kycCount = countKyc;

                    changeCounter("deposits", countDeposits);
                    changeCounter("withdrawals", countWithdrawals);
                    changeCounter("support", countSupport);
                    changeCounter("kyc", countKyc);

                    if (playSound) {
                        try {
                            let notificationSound = new Audio('../assets/media/notification.mp3');
                            notificationSound.loop = false;
                            notificationSound.volume = 0.5;
                            notificationSound.play();
                        } catch (error2) {
                        }
                    }
                } catch (error) {}
            }
        });
    }, 15000);

    function changeCounter(prefix, count) {
        try {
            $("#" + prefix + "_unviewed").text(count);
            if (count <= 0) {
                $("#" + prefix + "_unviewed").css('display', 'none');
            } else {
                $("#" + prefix + "_unviewed").css('display', 'flex');
            }
        } catch (error) {
            console.log(error);
        }
    }
});