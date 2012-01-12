#!/usr/bin/perl -w

use strict;

use Data::Dumper;

my $BEGIN_LVERSION = "BEGIN_LVERSION";
my $END_LVERSION = "END_LVERSION";
my $BEGIN_SVERSION = "BEGIN_SVERSION";
my $END_SVERSION = "END_SVERSION";


my $current_lversion = $ARGV[0];
my $new_lversion = $ARGV[1];

if (!defined($current_lversion) || !defined($new_lversion)) {
	print "Usage: $0 <current> <new>\n";
	exit(1);
}

my $current_sversion = $current_lversion;
$current_sversion =~ s/(\d+)\.(\d+).*/$1.$2/;
my $new_sversion = $new_lversion;
$new_sversion =~ s/(\d+)\.(\d+).*/$1.$2/;


print "Long: $current_lversion => $new_lversion\n";
print "Short: $current_sversion => $new_sversion\n";


my $files = get_files();
my @f = split(/\n/, $files);
$files = \@f;
#print Dumper($files);


my $nb_lines = 0;
for my $file (@$files) {
	$nb_lines += treat_file($file);
}

print "Modifié $nb_lines lignes\n";


exit(0);

##########################################################################################


sub get_files
{
	my $files = `find . -type f | grep -v ".svn" | grep -v "\/target\/"`;
	return $files;
}

sub treat_file
{
	my $file = shift;

	#print "Treating file: $file\n";

	open(FILE, "<$file");
	my @origs = <FILE>;
	chomp(@origs);
	close(FILE);

	my $in_long = 0;
	my $in_short = 0;
	my $nb_lines = 0;
	my @modifs = ();
	for my $orig (@origs) {

		my $modified = $orig;
		#print "B: $modified\n";

		# Long Version
		if ($modified =~ /$BEGIN_LVERSION/) {
			$in_long = 1;
		}
		if ($modified =~ /$END_LVERSION/) {
			$in_long = 0;
		}

		if ($in_long) {
			$modified =~ s/$current_lversion/$new_lversion/
		}

		# Short Version
		if ($modified =~ /$BEGIN_SVERSION/) {
			$in_short = 1;
		}
		if ($modified =~ /$END_SVERSION/) {
			$in_short = 0;
		}

		if ($in_short && $current_sversion != $new_sversion) {
			$modified =~ s/$current_sversion/$new_sversion/;
		}

		#print "A: $modified\n";
		if ($orig ne $modified) {
			print "* $file\nAv:\t$orig\nAp:\t$modified\n\n";
			$nb_lines += 1;
		}

		push(@modifs, "$modified\n");
	}

	if ($nb_lines > 0) {
		#print "Writing $file\n";
		open(FILE, ">$file");
		print FILE @modifs;
		close(FILE);
	}

	return $nb_lines;
}

