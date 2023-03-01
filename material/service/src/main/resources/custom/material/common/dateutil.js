const date_util = {
    day_mills: 24 * 60 * 60 * 1000,
    addDay: function (date, addition) {
        return new Date(date.valueOf() + addition * this.day_mills);
    },
    addMonth: function (date, addition) {
      date=new Date(date)
        var year = date.getFullYear();
        var month = date.getMonth();
        var monthTotal = month + addition;
        var yearAddition = Math.floor(monthTotal / 12);
        var toMonth = monthTotal % 12;
        toMonth = toMonth < 0 ? toMonth + 12 : toMonth;
        var toYear = year + yearAddition;
        return new Date(toYear, toMonth, date.getDate());
    },
    addYear: function (date, addition) {
        return new Date(date.getFullYear() + addition, date.getMonth(), date.getDate());
    },
    add: function (date, addition, type) {
        switch (type) {
            case "day": {
                return this.addDay(date, addition);
            }
            case "month": {
                return this.addMonth(date, addition);
            }
            case "year": {
                return this.addYear(date, addition);
            }
        }
    }
}