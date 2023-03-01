//js调用示例
function sendPostNormal(jsonParam, userName, companyCode) {
    var jsonParam = jsonParam || $("[name='apiinfosHead.apiJSON']").val();
    $.ajax({
        url: url,
        type: "post",
        data: {
            "jsonParam": jsonParam,
            "userName": userName,
            "companyCode": companyCode
        },
        success: function(msg) {
            console.log(msg);
        }
    });
}
