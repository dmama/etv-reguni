package ch.vd.uniregctb.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class GentilIteratorTest extends WithoutSpringTest {

	@Test
	public void testEmptyList() {
		GentilIterator<Object> iter = new GentilIterator<Object>(Collections.emptyList());
		assertFalse(iter.hasNext());
		assertFalse(iter.isFirst());
		assertFalse(iter.isLast());
		assertFalse(iter.isAtNewPercent());
	}

	@Test
	public void testSmallList() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(0);
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		GentilIterator<Integer> iter = new GentilIterator<Integer>(list);

		// 0
		assertTrue(iter.hasNext());
		assertEquals(Integer.valueOf(0), iter.next());
		assertTrue(iter.isFirst());
		assertFalse(iter.isLast());
		assertTrue(iter.isAtNewPercent());

		// 1
		assertTrue(iter.hasNext());
		assertEquals(Integer.valueOf(1), iter.next());
		assertFalse(iter.isFirst());
		assertFalse(iter.isLast());
		assertTrue(iter.isAtNewPercent());

		// 2
		assertTrue(iter.hasNext());
		assertEquals(Integer.valueOf(2), iter.next());
		assertFalse(iter.isFirst());
		assertFalse(iter.isLast());
		assertTrue(iter.isAtNewPercent());

		// 3
		assertTrue(iter.hasNext());
		assertEquals(Integer.valueOf(3), iter.next());
		assertFalse(iter.isFirst());
		assertFalse(iter.isLast());
		assertTrue(iter.isAtNewPercent());

		// 4
		assertTrue(iter.hasNext());
		assertEquals(Integer.valueOf(4), iter.next());
		assertFalse(iter.isFirst());
		assertTrue(iter.isLast());
		assertTrue(iter.isAtNewPercent());

		assertFalse(iter.hasNext());
	}

	private static class Data {

		public boolean isFirst;
		public boolean isLast;
		public int percent;
		public boolean isAtNewPercent;

		public Data(boolean isFirst, boolean isLast, int percent, boolean isAtPercent) {
			this.isFirst = isFirst;
			this.isLast = isLast;
			this.percent = percent;
			this.isAtNewPercent = isAtPercent;
		}
	}

	@Test
	public void testBigList() {

		final int size = 454;

		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < size; ++i) {
			list.add(i);
		}

		List<Data> expected = new ArrayList<Data>(size);
		expected.add(new Data(true, false, 0, true)); // 0
		expected.add(new Data(false, false, 0, false)); // 1
		expected.add(new Data(false, false, 0, false)); // 2
		expected.add(new Data(false, false, 0, false)); // 3
		expected.add(new Data(false, false, 0, false)); // 4
		expected.add(new Data(false, false, 1, true)); // 5
		expected.add(new Data(false, false, 1, false)); // 6
		expected.add(new Data(false, false, 1, false)); // 7
		expected.add(new Data(false, false, 1, false)); // 8
		expected.add(new Data(false, false, 1, false)); // 9
		expected.add(new Data(false, false, 2, true)); // 10
		expected.add(new Data(false, false, 2, false)); // 11
		expected.add(new Data(false, false, 2, false)); // 12
		expected.add(new Data(false, false, 2, false)); // 13
		expected.add(new Data(false, false, 3, true)); // 14
		expected.add(new Data(false, false, 3, false)); // 15
		expected.add(new Data(false, false, 3, false)); // 16
		expected.add(new Data(false, false, 3, false)); // 17
		expected.add(new Data(false, false, 3, false)); // 18
		expected.add(new Data(false, false, 4, true)); // 19
		expected.add(new Data(false, false, 4, false)); // 20
		expected.add(new Data(false, false, 4, false)); // 21
		expected.add(new Data(false, false, 4, false)); // 22
		expected.add(new Data(false, false, 5, true)); // 23
		expected.add(new Data(false, false, 5, false)); // 24
		expected.add(new Data(false, false, 5, false)); // 25
		expected.add(new Data(false, false, 5, false)); // 26
		expected.add(new Data(false, false, 5, false)); // 27
		expected.add(new Data(false, false, 6, true)); // 28
		expected.add(new Data(false, false, 6, false)); // 29
		expected.add(new Data(false, false, 6, false)); // 30
		expected.add(new Data(false, false, 6, false)); // 31
		expected.add(new Data(false, false, 7, true)); // 32
		expected.add(new Data(false, false, 7, false)); // 33
		expected.add(new Data(false, false, 7, false)); // 34
		expected.add(new Data(false, false, 7, false)); // 35
		expected.add(new Data(false, false, 7, false)); // 36
		expected.add(new Data(false, false, 8, true)); // 37
		expected.add(new Data(false, false, 8, false)); // 38
		expected.add(new Data(false, false, 8, false)); // 39
		expected.add(new Data(false, false, 8, false)); // 40
		expected.add(new Data(false, false, 9, true)); // 41
		expected.add(new Data(false, false, 9, false)); // 42
		expected.add(new Data(false, false, 9, false)); // 43
		expected.add(new Data(false, false, 9, false)); // 44
		expected.add(new Data(false, false, 9, false)); // 45
		expected.add(new Data(false, false, 10, true)); // 46
		expected.add(new Data(false, false, 10, false)); // 47
		expected.add(new Data(false, false, 10, false)); // 48
		expected.add(new Data(false, false, 10, false)); // 49
		expected.add(new Data(false, false, 11, true)); // 50
		expected.add(new Data(false, false, 11, false)); // 51
		expected.add(new Data(false, false, 11, false)); // 52
		expected.add(new Data(false, false, 11, false)); // 53
		expected.add(new Data(false, false, 11, false)); // 54
		expected.add(new Data(false, false, 12, true)); // 55
		expected.add(new Data(false, false, 12, false)); // 56
		expected.add(new Data(false, false, 12, false)); // 57
		expected.add(new Data(false, false, 12, false)); // 58
		expected.add(new Data(false, false, 12, false)); // 59
		expected.add(new Data(false, false, 13, true)); // 60
		expected.add(new Data(false, false, 13, false)); // 61
		expected.add(new Data(false, false, 13, false)); // 62
		expected.add(new Data(false, false, 13, false)); // 63
		expected.add(new Data(false, false, 14, true)); // 64
		expected.add(new Data(false, false, 14, false)); // 65
		expected.add(new Data(false, false, 14, false)); // 66
		expected.add(new Data(false, false, 14, false)); // 67
		expected.add(new Data(false, false, 14, false)); // 68
		expected.add(new Data(false, false, 15, true)); // 69
		expected.add(new Data(false, false, 15, false)); // 70
		expected.add(new Data(false, false, 15, false)); // 71
		expected.add(new Data(false, false, 15, false)); // 72
		expected.add(new Data(false, false, 16, true)); // 73
		expected.add(new Data(false, false, 16, false)); // 74
		expected.add(new Data(false, false, 16, false)); // 75
		expected.add(new Data(false, false, 16, false)); // 76
		expected.add(new Data(false, false, 16, false)); // 77
		expected.add(new Data(false, false, 17, true)); // 78
		expected.add(new Data(false, false, 17, false)); // 79
		expected.add(new Data(false, false, 17, false)); // 80
		expected.add(new Data(false, false, 17, false)); // 81
		expected.add(new Data(false, false, 18, true)); // 82
		expected.add(new Data(false, false, 18, false)); // 83
		expected.add(new Data(false, false, 18, false)); // 84
		expected.add(new Data(false, false, 18, false)); // 85
		expected.add(new Data(false, false, 18, false)); // 86
		expected.add(new Data(false, false, 19, true)); // 87
		expected.add(new Data(false, false, 19, false)); // 88
		expected.add(new Data(false, false, 19, false)); // 89
		expected.add(new Data(false, false, 19, false)); // 90
		expected.add(new Data(false, false, 20, true)); // 91
		expected.add(new Data(false, false, 20, false)); // 92
		expected.add(new Data(false, false, 20, false)); // 93
		expected.add(new Data(false, false, 20, false)); // 94
		expected.add(new Data(false, false, 20, false)); // 95
		expected.add(new Data(false, false, 21, true)); // 96
		expected.add(new Data(false, false, 21, false)); // 97
		expected.add(new Data(false, false, 21, false)); // 98
		expected.add(new Data(false, false, 21, false)); // 99
		expected.add(new Data(false, false, 22, true)); // 100
		expected.add(new Data(false, false, 22, false)); // 101
		expected.add(new Data(false, false, 22, false)); // 102
		expected.add(new Data(false, false, 22, false)); // 103
		expected.add(new Data(false, false, 22, false)); // 104
		expected.add(new Data(false, false, 23, true)); // 105
		expected.add(new Data(false, false, 23, false)); // 106
		expected.add(new Data(false, false, 23, false)); // 107
		expected.add(new Data(false, false, 23, false)); // 108
		expected.add(new Data(false, false, 24, true)); // 109
		expected.add(new Data(false, false, 24, false)); // 110
		expected.add(new Data(false, false, 24, false)); // 111
		expected.add(new Data(false, false, 24, false)); // 112
		expected.add(new Data(false, false, 24, false)); // 113
		expected.add(new Data(false, false, 25, true)); // 114
		expected.add(new Data(false, false, 25, false)); // 115
		expected.add(new Data(false, false, 25, false)); // 116
		expected.add(new Data(false, false, 25, false)); // 117
		expected.add(new Data(false, false, 25, false)); // 118
		expected.add(new Data(false, false, 26, true)); // 119
		expected.add(new Data(false, false, 26, false)); // 120
		expected.add(new Data(false, false, 26, false)); // 121
		expected.add(new Data(false, false, 26, false)); // 122
		expected.add(new Data(false, false, 27, true)); // 123
		expected.add(new Data(false, false, 27, false)); // 124
		expected.add(new Data(false, false, 27, false)); // 125
		expected.add(new Data(false, false, 27, false)); // 126
		expected.add(new Data(false, false, 27, false)); // 127
		expected.add(new Data(false, false, 28, true)); // 128
		expected.add(new Data(false, false, 28, false)); // 129
		expected.add(new Data(false, false, 28, false)); // 130
		expected.add(new Data(false, false, 28, false)); // 131
		expected.add(new Data(false, false, 29, true)); // 132
		expected.add(new Data(false, false, 29, false)); // 133
		expected.add(new Data(false, false, 29, false)); // 134
		expected.add(new Data(false, false, 29, false)); // 135
		expected.add(new Data(false, false, 29, false)); // 136
		expected.add(new Data(false, false, 30, true)); // 137
		expected.add(new Data(false, false, 30, false)); // 138
		expected.add(new Data(false, false, 30, false)); // 139
		expected.add(new Data(false, false, 30, false)); // 140
		expected.add(new Data(false, false, 31, true)); // 141
		expected.add(new Data(false, false, 31, false)); // 142
		expected.add(new Data(false, false, 31, false)); // 143
		expected.add(new Data(false, false, 31, false)); // 144
		expected.add(new Data(false, false, 31, false)); // 145
		expected.add(new Data(false, false, 32, true)); // 146
		expected.add(new Data(false, false, 32, false)); // 147
		expected.add(new Data(false, false, 32, false)); // 148
		expected.add(new Data(false, false, 32, false)); // 149
		expected.add(new Data(false, false, 33, true)); // 150
		expected.add(new Data(false, false, 33, false)); // 151
		expected.add(new Data(false, false, 33, false)); // 152
		expected.add(new Data(false, false, 33, false)); // 153
		expected.add(new Data(false, false, 33, false)); // 154
		expected.add(new Data(false, false, 34, true)); // 155
		expected.add(new Data(false, false, 34, false)); // 156
		expected.add(new Data(false, false, 34, false)); // 157
		expected.add(new Data(false, false, 34, false)); // 158
		expected.add(new Data(false, false, 35, true)); // 159
		expected.add(new Data(false, false, 35, false)); // 160
		expected.add(new Data(false, false, 35, false)); // 161
		expected.add(new Data(false, false, 35, false)); // 162
		expected.add(new Data(false, false, 35, false)); // 163
		expected.add(new Data(false, false, 36, true)); // 164
		expected.add(new Data(false, false, 36, false)); // 165
		expected.add(new Data(false, false, 36, false)); // 166
		expected.add(new Data(false, false, 36, false)); // 167
		expected.add(new Data(false, false, 37, true)); // 168
		expected.add(new Data(false, false, 37, false)); // 169
		expected.add(new Data(false, false, 37, false)); // 170
		expected.add(new Data(false, false, 37, false)); // 171
		expected.add(new Data(false, false, 37, false)); // 172
		expected.add(new Data(false, false, 38, true)); // 173
		expected.add(new Data(false, false, 38, false)); // 174
		expected.add(new Data(false, false, 38, false)); // 175
		expected.add(new Data(false, false, 38, false)); // 176
		expected.add(new Data(false, false, 38, false)); // 177
		expected.add(new Data(false, false, 39, true)); // 178
		expected.add(new Data(false, false, 39, false)); // 179
		expected.add(new Data(false, false, 39, false)); // 180
		expected.add(new Data(false, false, 39, false)); // 181
		expected.add(new Data(false, false, 40, true)); // 182
		expected.add(new Data(false, false, 40, false)); // 183
		expected.add(new Data(false, false, 40, false)); // 184
		expected.add(new Data(false, false, 40, false)); // 185
		expected.add(new Data(false, false, 40, false)); // 186
		expected.add(new Data(false, false, 41, true)); // 187
		expected.add(new Data(false, false, 41, false)); // 188
		expected.add(new Data(false, false, 41, false)); // 189
		expected.add(new Data(false, false, 41, false)); // 190
		expected.add(new Data(false, false, 42, true)); // 191
		expected.add(new Data(false, false, 42, false)); // 192
		expected.add(new Data(false, false, 42, false)); // 193
		expected.add(new Data(false, false, 42, false)); // 194
		expected.add(new Data(false, false, 42, false)); // 195
		expected.add(new Data(false, false, 43, true)); // 196
		expected.add(new Data(false, false, 43, false)); // 197
		expected.add(new Data(false, false, 43, false)); // 198
		expected.add(new Data(false, false, 43, false)); // 199
		expected.add(new Data(false, false, 44, true)); // 200
		expected.add(new Data(false, false, 44, false)); // 201
		expected.add(new Data(false, false, 44, false)); // 202
		expected.add(new Data(false, false, 44, false)); // 203
		expected.add(new Data(false, false, 44, false)); // 204
		expected.add(new Data(false, false, 45, true)); // 205
		expected.add(new Data(false, false, 45, false)); // 206
		expected.add(new Data(false, false, 45, false)); // 207
		expected.add(new Data(false, false, 45, false)); // 208
		expected.add(new Data(false, false, 46, true)); // 209
		expected.add(new Data(false, false, 46, false)); // 210
		expected.add(new Data(false, false, 46, false)); // 211
		expected.add(new Data(false, false, 46, false)); // 212
		expected.add(new Data(false, false, 46, false)); // 213
		expected.add(new Data(false, false, 47, true)); // 214
		expected.add(new Data(false, false, 47, false)); // 215
		expected.add(new Data(false, false, 47, false)); // 216
		expected.add(new Data(false, false, 47, false)); // 217
		expected.add(new Data(false, false, 48, true)); // 218
		expected.add(new Data(false, false, 48, false)); // 219
		expected.add(new Data(false, false, 48, false)); // 220
		expected.add(new Data(false, false, 48, false)); // 221
		expected.add(new Data(false, false, 48, false)); // 222
		expected.add(new Data(false, false, 49, true)); // 223
		expected.add(new Data(false, false, 49, false)); // 224
		expected.add(new Data(false, false, 49, false)); // 225
		expected.add(new Data(false, false, 49, false)); // 226
		expected.add(new Data(false, false, 50, true)); // 227
		expected.add(new Data(false, false, 50, false)); // 228
		expected.add(new Data(false, false, 50, false)); // 229
		expected.add(new Data(false, false, 50, false)); // 230
		expected.add(new Data(false, false, 50, false)); // 231
		expected.add(new Data(false, false, 51, true)); // 232
		expected.add(new Data(false, false, 51, false)); // 233
		expected.add(new Data(false, false, 51, false)); // 234
		expected.add(new Data(false, false, 51, false)); // 235
		expected.add(new Data(false, false, 51, false)); // 236
		expected.add(new Data(false, false, 52, true)); // 237
		expected.add(new Data(false, false, 52, false)); // 238
		expected.add(new Data(false, false, 52, false)); // 239
		expected.add(new Data(false, false, 52, false)); // 240
		expected.add(new Data(false, false, 53, true)); // 241
		expected.add(new Data(false, false, 53, false)); // 242
		expected.add(new Data(false, false, 53, false)); // 243
		expected.add(new Data(false, false, 53, false)); // 244
		expected.add(new Data(false, false, 53, false)); // 245
		expected.add(new Data(false, false, 54, true)); // 246
		expected.add(new Data(false, false, 54, false)); // 247
		expected.add(new Data(false, false, 54, false)); // 248
		expected.add(new Data(false, false, 54, false)); // 249
		expected.add(new Data(false, false, 55, true)); // 250
		expected.add(new Data(false, false, 55, false)); // 251
		expected.add(new Data(false, false, 55, false)); // 252
		expected.add(new Data(false, false, 55, false)); // 253
		expected.add(new Data(false, false, 55, false)); // 254
		expected.add(new Data(false, false, 56, true)); // 255
		expected.add(new Data(false, false, 56, false)); // 256
		expected.add(new Data(false, false, 56, false)); // 257
		expected.add(new Data(false, false, 56, false)); // 258
		expected.add(new Data(false, false, 57, true)); // 259
		expected.add(new Data(false, false, 57, false)); // 260
		expected.add(new Data(false, false, 57, false)); // 261
		expected.add(new Data(false, false, 57, false)); // 262
		expected.add(new Data(false, false, 57, false)); // 263
		expected.add(new Data(false, false, 58, true)); // 264
		expected.add(new Data(false, false, 58, false)); // 265
		expected.add(new Data(false, false, 58, false)); // 266
		expected.add(new Data(false, false, 58, false)); // 267
		expected.add(new Data(false, false, 59, true)); // 268
		expected.add(new Data(false, false, 59, false)); // 269
		expected.add(new Data(false, false, 59, false)); // 270
		expected.add(new Data(false, false, 59, false)); // 271
		expected.add(new Data(false, false, 59, false)); // 272
		expected.add(new Data(false, false, 60, true)); // 273
		expected.add(new Data(false, false, 60, false)); // 274
		expected.add(new Data(false, false, 60, false)); // 275
		expected.add(new Data(false, false, 60, false)); // 276
		expected.add(new Data(false, false, 61, true)); // 277
		expected.add(new Data(false, false, 61, false)); // 278
		expected.add(new Data(false, false, 61, false)); // 279
		expected.add(new Data(false, false, 61, false)); // 280
		expected.add(new Data(false, false, 61, false)); // 281
		expected.add(new Data(false, false, 62, true)); // 282
		expected.add(new Data(false, false, 62, false)); // 283
		expected.add(new Data(false, false, 62, false)); // 284
		expected.add(new Data(false, false, 62, false)); // 285
		expected.add(new Data(false, false, 62, false)); // 286
		expected.add(new Data(false, false, 63, true)); // 287
		expected.add(new Data(false, false, 63, false)); // 288
		expected.add(new Data(false, false, 63, false)); // 289
		expected.add(new Data(false, false, 63, false)); // 290
		expected.add(new Data(false, false, 64, true)); // 291
		expected.add(new Data(false, false, 64, false)); // 292
		expected.add(new Data(false, false, 64, false)); // 293
		expected.add(new Data(false, false, 64, false)); // 294
		expected.add(new Data(false, false, 64, false)); // 295
		expected.add(new Data(false, false, 65, true)); // 296
		expected.add(new Data(false, false, 65, false)); // 297
		expected.add(new Data(false, false, 65, false)); // 298
		expected.add(new Data(false, false, 65, false)); // 299
		expected.add(new Data(false, false, 66, true)); // 300
		expected.add(new Data(false, false, 66, false)); // 301
		expected.add(new Data(false, false, 66, false)); // 302
		expected.add(new Data(false, false, 66, false)); // 303
		expected.add(new Data(false, false, 66, false)); // 304
		expected.add(new Data(false, false, 67, true)); // 305
		expected.add(new Data(false, false, 67, false)); // 306
		expected.add(new Data(false, false, 67, false)); // 307
		expected.add(new Data(false, false, 67, false)); // 308
		expected.add(new Data(false, false, 68, true)); // 309
		expected.add(new Data(false, false, 68, false)); // 310
		expected.add(new Data(false, false, 68, false)); // 311
		expected.add(new Data(false, false, 68, false)); // 312
		expected.add(new Data(false, false, 68, false)); // 313
		expected.add(new Data(false, false, 69, true)); // 314
		expected.add(new Data(false, false, 69, false)); // 315
		expected.add(new Data(false, false, 69, false)); // 316
		expected.add(new Data(false, false, 69, false)); // 317
		expected.add(new Data(false, false, 70, true)); // 318
		expected.add(new Data(false, false, 70, false)); // 319
		expected.add(new Data(false, false, 70, false)); // 320
		expected.add(new Data(false, false, 70, false)); // 321
		expected.add(new Data(false, false, 70, false)); // 322
		expected.add(new Data(false, false, 71, true)); // 323
		expected.add(new Data(false, false, 71, false)); // 324
		expected.add(new Data(false, false, 71, false)); // 325
		expected.add(new Data(false, false, 71, false)); // 326
		expected.add(new Data(false, false, 72, true)); // 327
		expected.add(new Data(false, false, 72, false)); // 328
		expected.add(new Data(false, false, 72, false)); // 329
		expected.add(new Data(false, false, 72, false)); // 330
		expected.add(new Data(false, false, 72, false)); // 331
		expected.add(new Data(false, false, 73, true)); // 332
		expected.add(new Data(false, false, 73, false)); // 333
		expected.add(new Data(false, false, 73, false)); // 334
		expected.add(new Data(false, false, 73, false)); // 335
		expected.add(new Data(false, false, 74, true)); // 336
		expected.add(new Data(false, false, 74, false)); // 337
		expected.add(new Data(false, false, 74, false)); // 338
		expected.add(new Data(false, false, 74, false)); // 339
		expected.add(new Data(false, false, 74, false)); // 340
		expected.add(new Data(false, false, 75, true)); // 341
		expected.add(new Data(false, false, 75, false)); // 342
		expected.add(new Data(false, false, 75, false)); // 343
		expected.add(new Data(false, false, 75, false)); // 344
		expected.add(new Data(false, false, 75, false)); // 345
		expected.add(new Data(false, false, 76, true)); // 346
		expected.add(new Data(false, false, 76, false)); // 347
		expected.add(new Data(false, false, 76, false)); // 348
		expected.add(new Data(false, false, 76, false)); // 349
		expected.add(new Data(false, false, 77, true)); // 350
		expected.add(new Data(false, false, 77, false)); // 351
		expected.add(new Data(false, false, 77, false)); // 352
		expected.add(new Data(false, false, 77, false)); // 353
		expected.add(new Data(false, false, 77, false)); // 354
		expected.add(new Data(false, false, 78, true)); // 355
		expected.add(new Data(false, false, 78, false)); // 356
		expected.add(new Data(false, false, 78, false)); // 357
		expected.add(new Data(false, false, 78, false)); // 358
		expected.add(new Data(false, false, 79, true)); // 359
		expected.add(new Data(false, false, 79, false)); // 360
		expected.add(new Data(false, false, 79, false)); // 361
		expected.add(new Data(false, false, 79, false)); // 362
		expected.add(new Data(false, false, 79, false)); // 363
		expected.add(new Data(false, false, 80, true)); // 364
		expected.add(new Data(false, false, 80, false)); // 365
		expected.add(new Data(false, false, 80, false)); // 366
		expected.add(new Data(false, false, 80, false)); // 367
		expected.add(new Data(false, false, 81, true)); // 368
		expected.add(new Data(false, false, 81, false)); // 369
		expected.add(new Data(false, false, 81, false)); // 370
		expected.add(new Data(false, false, 81, false)); // 371
		expected.add(new Data(false, false, 81, false)); // 372
		expected.add(new Data(false, false, 82, true)); // 373
		expected.add(new Data(false, false, 82, false)); // 374
		expected.add(new Data(false, false, 82, false)); // 375
		expected.add(new Data(false, false, 82, false)); // 376
		expected.add(new Data(false, false, 83, true)); // 377
		expected.add(new Data(false, false, 83, false)); // 378
		expected.add(new Data(false, false, 83, false)); // 379
		expected.add(new Data(false, false, 83, false)); // 380
		expected.add(new Data(false, false, 83, false)); // 381
		expected.add(new Data(false, false, 84, true)); // 382
		expected.add(new Data(false, false, 84, false)); // 383
		expected.add(new Data(false, false, 84, false)); // 384
		expected.add(new Data(false, false, 84, false)); // 385
		expected.add(new Data(false, false, 85, true)); // 386
		expected.add(new Data(false, false, 85, false)); // 387
		expected.add(new Data(false, false, 85, false)); // 388
		expected.add(new Data(false, false, 85, false)); // 389
		expected.add(new Data(false, false, 85, false)); // 390
		expected.add(new Data(false, false, 86, true)); // 391
		expected.add(new Data(false, false, 86, false)); // 392
		expected.add(new Data(false, false, 86, false)); // 393
		expected.add(new Data(false, false, 86, false)); // 394
		expected.add(new Data(false, false, 87, true)); // 395
		expected.add(new Data(false, false, 87, false)); // 396
		expected.add(new Data(false, false, 87, false)); // 397
		expected.add(new Data(false, false, 87, false)); // 398
		expected.add(new Data(false, false, 87, false)); // 399
		expected.add(new Data(false, false, 88, true)); // 400
		expected.add(new Data(false, false, 88, false)); // 401
		expected.add(new Data(false, false, 88, false)); // 402
		expected.add(new Data(false, false, 88, false)); // 403
		expected.add(new Data(false, false, 88, false)); // 404
		expected.add(new Data(false, false, 89, true)); // 405
		expected.add(new Data(false, false, 89, false)); // 406
		expected.add(new Data(false, false, 89, false)); // 407
		expected.add(new Data(false, false, 89, false)); // 408
		expected.add(new Data(false, false, 90, true)); // 409
		expected.add(new Data(false, false, 90, false)); // 410
		expected.add(new Data(false, false, 90, false)); // 411
		expected.add(new Data(false, false, 90, false)); // 412
		expected.add(new Data(false, false, 90, false)); // 413
		expected.add(new Data(false, false, 91, true)); // 414
		expected.add(new Data(false, false, 91, false)); // 415
		expected.add(new Data(false, false, 91, false)); // 416
		expected.add(new Data(false, false, 91, false)); // 417
		expected.add(new Data(false, false, 92, true)); // 418
		expected.add(new Data(false, false, 92, false)); // 419
		expected.add(new Data(false, false, 92, false)); // 420
		expected.add(new Data(false, false, 92, false)); // 421
		expected.add(new Data(false, false, 92, false)); // 422
		expected.add(new Data(false, false, 93, true)); // 423
		expected.add(new Data(false, false, 93, false)); // 424
		expected.add(new Data(false, false, 93, false)); // 425
		expected.add(new Data(false, false, 93, false)); // 426
		expected.add(new Data(false, false, 94, true)); // 427
		expected.add(new Data(false, false, 94, false)); // 428
		expected.add(new Data(false, false, 94, false)); // 429
		expected.add(new Data(false, false, 94, false)); // 430
		expected.add(new Data(false, false, 94, false)); // 431
		expected.add(new Data(false, false, 95, true)); // 432
		expected.add(new Data(false, false, 95, false)); // 433
		expected.add(new Data(false, false, 95, false)); // 434
		expected.add(new Data(false, false, 95, false)); // 435
		expected.add(new Data(false, false, 96, true)); // 436
		expected.add(new Data(false, false, 96, false)); // 437
		expected.add(new Data(false, false, 96, false)); // 438
		expected.add(new Data(false, false, 96, false)); // 439
		expected.add(new Data(false, false, 96, false)); // 440
		expected.add(new Data(false, false, 97, true)); // 441
		expected.add(new Data(false, false, 97, false)); // 442
		expected.add(new Data(false, false, 97, false)); // 443
		expected.add(new Data(false, false, 97, false)); // 444
		expected.add(new Data(false, false, 98, true)); // 445
		expected.add(new Data(false, false, 98, false)); // 446
		expected.add(new Data(false, false, 98, false)); // 447
		expected.add(new Data(false, false, 98, false)); // 448
		expected.add(new Data(false, false, 98, false)); // 449
		expected.add(new Data(false, false, 99, true)); // 450
		expected.add(new Data(false, false, 99, false)); // 451
		expected.add(new Data(false, false, 99, false)); // 452
		expected.add(new Data(false, true, 99, false)); // 453
		assertEquals(size, expected.size());

		GentilIterator<Integer> iter = new GentilIterator<Integer>(list);

		for (int i = 0; i < size; ++i) {
			Data d = expected.get(i);
			assertTrue("index=" + i, iter.hasNext());
			assertEquals("index=" + i, Integer.valueOf(i), iter.next());
			assertEquals("index=" + i, d.isFirst, iter.isFirst());
			assertEquals("index=" + i, d.isLast, iter.isLast());
			assertEquals("index=" + i, d.percent, iter.getPercent());
			assertEquals("index=" + i, d.isAtNewPercent, iter.isAtNewPercent());
		}
		assertFalse(iter.hasNext());
	}
}
