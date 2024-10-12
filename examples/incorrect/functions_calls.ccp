let g = [x: Int, y: Bool] -> Bool => x < 5 && y; # correct definition
# wrong calls:
g 4 false;
g 4, false;
g[4 false];
