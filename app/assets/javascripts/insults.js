$(function() {
    $('#insults input[type=submit]').click(function(e) {
        e.preventDefault();
        $.post('/insults', {}, function(data, status, xhr) {
            $('#insults #result').text(data);
        });
    });
});
