import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  type ViewStyle,
} from 'react-native';

import { colors } from '@/src/theme/colors';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';

type Props = {
  label: string;
  onPress: () => void;
  variant?: Variant;
  disabled?: boolean;
  loading?: boolean;
  style?: ViewStyle;
};

export function PrimaryButton({
  label,
  onPress,
  variant = 'primary',
  disabled,
  loading,
  style,
}: Props) {
  const palette =
    variant === 'primary'
      ? styles.primary
      : variant === 'secondary'
        ? styles.secondary
        : variant === 'danger'
          ? styles.danger
          : styles.ghost;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled || loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        palette,
        (disabled || loading) && styles.disabled,
        pressed && styles.pressed,
        style,
      ]}>
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? colors.white : colors.leaf} />
      ) : (
        <Text
          style={[
            styles.label,
            variant === 'primary' || variant === 'danger' ? styles.labelOnDark : styles.labelOnLight,
          ]}>
          {label}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 54,
    borderRadius: 16,
    paddingHorizontal: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primary: {
    backgroundColor: colors.leaf,
  },
  secondary: {
    backgroundColor: colors.rustSoft,
    borderWidth: 1,
    borderColor: colors.rust,
  },
  ghost: {
    backgroundColor: colors.bgElevated,
    borderWidth: 1,
    borderColor: colors.border,
  },
  danger: {
    backgroundColor: colors.danger,
  },
  disabled: {
    opacity: 0.5,
  },
  pressed: {
    opacity: 0.88,
    transform: [{ scale: 0.99 }],
  },
  label: {
    fontSize: 17,
    fontWeight: '700',
  },
  labelOnDark: {
    color: colors.white,
  },
  labelOnLight: {
    color: colors.ink,
  },
});
