import { Image } from 'expo-image';
import { StyleSheet, Text, View } from 'react-native';

import { colors } from '@/src/theme/colors';

type Props = {
  size?: number;
  showWordmark?: boolean;
};

export function LeafBrand({ size = 56, showWordmark = true }: Props) {
  return (
    <View style={styles.wrap}>
      <Image
        source={require('../../assets/icons/leaf.png')}
        style={{ width: size, height: size }}
        contentFit="contain"
      />
      {showWordmark ? <Text style={styles.wordmark}>LeafRust</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    alignItems: 'center',
    gap: 8,
  },
  wordmark: {
    fontSize: 34,
    fontWeight: '800',
    letterSpacing: -0.5,
    color: colors.leaf,
  },
});
