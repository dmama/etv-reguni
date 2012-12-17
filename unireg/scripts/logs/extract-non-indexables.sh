#! /bin/bash -

grep "\bMass-[0-9]\b" "$@" | grep "indexer le[s]\? tiers" | sed -e 's/^.*tiers n.//' | sed -e '/^[^0-9]/ s/[^0-9 ]\+//g' | sed -e '/:/! s/ /\n/g' | sort -un | while read LINE; do
	if [[ "$LINE" =~ ^[0-9]+$ ]]; then
		EXPL=$(grep -h "^[[:blank:]]\+$LINE" "$@"| sort -u | sed -e 's/^[[:blank:]]\+//')
		if [ -z "$EXPL" ]; then
			echo "$LINE"
		else
			echo "$EXPL"
		fi
	else
		echo "$LINE"
	fi
done | sed -e '/ServiceCivilException/ s/[a-zA-Z.]\+ServiceCivilException: //'
