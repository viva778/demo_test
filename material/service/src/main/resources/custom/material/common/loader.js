const loader = {
    __loadUrlContent: function (url, successCallback, errorCallback) {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", url, false);
        if ("overrideMimeType" in xhr) {
            xhr.overrideMimeType("text/plain");
        }
        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status === 0 || xhr.status === 200) {
                    successCallback(xhr.responseText);
                } else {
                    errorCallback && errorCallback();
                }
            }
        };
        try {
            xhr.send(null);
        } catch (ignored) { }
    },
    __addContent: function (content) {
        var parentEl = document.getElementsByTagName('head')[0];
        var s = document.createElement('script');
        s.text = content;
        parentEl.appendChild(s);
    },
    import: function (script) {
        var gdUrl = "/greenDill/static/material/common/" + script + ".js?v=" + Math.random();
        loader.__loadUrlContent(gdUrl, loader.__addContent, () => {
            var msUrl = "/msService/material/common/" + script + ".js?v=" + Math.random();
            loader.__loadUrlContent(msUrl, loader.__addContent, () => {
                console.error("脚本 '" + script + "' 加载失败");
            });
        });
    }
}