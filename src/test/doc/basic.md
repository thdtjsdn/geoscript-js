js> var geom = require("geoscript/geom");
js> var p = new geom.Point([0, 0]);
js> p.buffer(1).area.toFixed(4);
3.1214