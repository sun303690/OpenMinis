# Note: This file is included only for targets which have pokedata workaround
# Portable hex->decimal (mawk has no strtonum); only used at build time.
function hex2dec(h,   i, c, n) {
	h = tolower(h)
	sub(/^0x/, "", h)
	n = 0
	for (i = 1; i <= length(h); i++) {
		c = substr(h, i, 1)
		n = n * 16
		if (c >= "0" && c <= "9") n += c - "0"
		else if (c >= "a" && c <= "f") n += (c - "a" + 10)
	}
	return n
}
/\ypokedata_workaround\y/{pokedata_workaround=hex2dec($2)}
/\y_start\y/{start=hex2dec($2)}
END {
	print "#include <unistd.h>"
	print "const ssize_t offset_to_pokedata_workaround=" (pokedata_workaround-start) ";"
}
