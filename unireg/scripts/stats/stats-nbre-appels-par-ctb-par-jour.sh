#! /bin/bash -

echo "DATE;NB_CTB;NB_CALLS"

SUM_CALLS=0
NB_CTB=0
OLD_DATE=""
while read NB DATE CTB; do

	if [ "$DATE" != "$OLD_DATE" ]; then
		if [ "$SUM_CALLS" -gt 0 ]; then
			echo "$OLD_DATE;$NB_CTB;$SUM_CALLS"
		fi
		OLD_DATE="$DATE"
		NB_CTB=0
		SUM_CALLS=0
	fi
	((++NB_CTB))
	((SUM_CALLS+=$NB))

done < <(grep -H "tiersNumber=" "$@" | sed -e 's/:/\t/' -e 's/^.*\.\([0-9]\{4\}\)/\1/' | sed -e 's/^\([^\t]\+\).*tiersNumber=\([0-9]\+\).*$/\1\t\2/' | sort | uniq -c)

if [ "$SUM_CALLS" -gt 0 ]; then
	echo "$OLD_DATE;$NB_CTB;$SUM_CALLS"
fi
