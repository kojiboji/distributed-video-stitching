#'segment_list name'
#Generate also a listfile named name. If not specified no listfile is generated.

#'segment_list_entry_prefix prefix'
#Prepend prefix to each entry. Useful to generate absolute paths. By default no prefix is applied.

#'segment_list_type type'

#'segment_time time'
#Set segment duration to time, the value must be a duration specification. Default value is "2". See also the 'segment_times' option.
#Note that splitting may not be accurate, unless you force the reference stream key-frames at the given time. See the introductory notice and the examples below.

#'segment_time_delta delta'
#Specify the accuracy time when selecting the start time for a segment, expressed as a duration specification. Default value is "0".
#
#When delta is specified a key-frame will start a new segment if its PTS satisfies the relation:
#
#PTS >= start_time - time_delta
#This option is useful when splitting video content, which is always split at GOP boundaries, in case a key frame is found just before the specified split time.
#
#In particular may be used in combination with the 'ffmpeg' option force_key_frames. The key frame times specified by force_key_frames may not be set accurately because of rounding issues, with the consequence that a key frame time may result set just before the specified time. For constant frame rate videos a value of 1/(2*frame_rate) should address the worst case mismatch between the specified time and the time set by force_key_frames.
key_frames=$(ffprobe \
  -hide_banner -loglevel error \
  -select_streams v \
  -show_frames \
  -skip_frame nokey \
  -show_entries 'frame=best_effort_timestamp_time' \
  -of csv \
  $1 |
cut -d ',' -f 2 |
grep -E '^[\.0-9]+$')

key_frames=($key_frames)

segment_end=$2

for i in "${key_frames[@]}"
do
  less_than=$(echo "$i < $segment_end" | bc)
  echo "$i"
  echo "$less_than"
done


