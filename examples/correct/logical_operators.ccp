#basic logical operators

false||true;
false&&true;
!false;

#operators on boolean variables

let a: Bool = true;
let b: Bool = false;

a||b;
a&&b;
!a;

#nested operators

(a||b)&&(!(true&&false));

#logical operators should return a value

b = false;
let c: Bool = !(true && false);

#should we have any other logical operator, like xor?