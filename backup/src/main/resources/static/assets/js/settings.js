document.addEventListener('DOMContentLoaded', function() {
    $("#avatar_remove_btn").on('click', () => {
        $.ajax({
            url: "../api/user/settings",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                action: 'REMOVE_PROFILE_PHOTO'
            }),
            success: function (response) {
                if (response === 'user.api.removed.photo') {
                    noti(getMessage(response), 'success');
                    setTimeout(() => {
                        location.reload()
                    }, 1000);
                } else {
                    noti(getMessage(response), 'error');
                }
            }
        })
    });

    $("#avatar_upload_btn").on('click', function (event) {
        event.preventDefault();

        $("#profile_photo_input").click();
    });

    $("#profile_photo_input").on('change', function() {
        var photo_input = $(this).val();

        if (photo_input === '') {
            return;
        }

        var profile_photo = $("#profile_photo_input")[0].files[0];

        var form = new FormData();
        form.append('image', profile_photo);

        $.ajax({
            url: "../api/user/updatePhoto",
            data: form,
            cache: false,
            contentType: false,
            processData: false,
            type: 'POST',
            success: function (response) {
                if (response === 'user.api.changed.photo') {
                    noti(getMessage(response), 'success');
                    $("#profile_photo_input").val("");
                    setTimeout(() => {
                        location.reload()
                    }, 1000);
                } else {
                    noti(getMessage(response), 'error');
                }
            }
        });
    });

    waitingMessagesLoading();

    function onMessagesLoaded() {
        const uidEl = $('.settings-account-info-uid');
        copyWithButton(uidEl.find('svg'), getMessage('profile.uid.copied'), () => copyToClipboard(uidEl.text().split(' ')[1].trim()));
    }

    function waitingMessagesLoading() {
        if (messagesLoaded) {
            onMessagesLoaded();
            return;
        }

        setTimeout(() => {
            waitingMessagesLoading();
        }, 50);
    }
});